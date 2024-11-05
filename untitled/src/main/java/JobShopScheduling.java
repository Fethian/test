import java.util.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;

import java.awt.*;
import java.io.File;
import java.util.List;

public class JobShopScheduling {

    static class Operation {
        int jobId;
        int operationId;
        int machineId;
        int processingTime;
        int startTime;
        int endTime;

        public Operation(int jobId, int operationId, int machineId, int processingTime) {
            this.jobId = jobId;
            this.operationId = operationId;
            this.machineId = machineId;
            this.processingTime = processingTime;
            this.startTime = -1;
            this.endTime = -1;
        }
    }

    static class Machine {
        int id;
        int availableTime;

        public Machine(int id) {
            this.id = id;
            this.availableTime = 0;
        }
    }

    static class Job {
        int id;
        List<Operation> operations;

        public Job(int id) {
            this.id = id;
            this.operations = new ArrayList<>();
        }
    }

    static int minMakespan = Integer.MAX_VALUE;
    static List<Operation> bestSchedule = null;
    static Map<Integer, Machine> machines = new HashMap<>();
    static Map<Integer, Job> jobs = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // 初始化机器
        for (int i = 0; i <= 2; i++) {
            machines.put(i, new Machine(i));
        }

        // 初始化作业和工序
        Job job0 = new Job(0);
        job0.operations.add(new Operation(0, 0, 0, 3));
        job0.operations.add(new Operation(0, 1, 1, 2));
        job0.operations.add(new Operation(0, 2, 2, 2));
        jobs.put(0, job0);

        Job job1 = new Job(1);
        job1.operations.add(new Operation(1, 0, 0, 2));
        job1.operations.add(new Operation(1, 1, 2, 1));
        job1.operations.add(new Operation(1, 2, 1, 4));
        jobs.put(1, job1);

        Job job2 = new Job(2);
        job2.operations.add(new Operation(2, 0, 1, 4));
        job2.operations.add(new Operation(2, 1, 2, 3));
        jobs.put(2, job2);

        List<Operation> schedule = new ArrayList<>();

        backtrack(schedule);

        // 输出最优调度结果
        System.out.println("最优完工时间：" + minMakespan);
        for (Operation op : bestSchedule) {
            System.out.println("作业" + op.jobId + " 工序" + op.operationId + "：开始时间 " + op.startTime + "，结束时间 " + op.endTime + "，机器 " + op.machineId);
        }

        // 生成甘特图
        generateGanttChart(bestSchedule);
    }

    static void backtrack(List<Operation> schedule) {
        if (schedule.size() == 8) {
            int makespan = calculateMakespan(schedule);
            if (makespan < minMakespan) {
                minMakespan = makespan;
                bestSchedule = new ArrayList<>(schedule);
            }
            return;
        }

        for (Job job : jobs.values()) {
            if (job.operations.size() > 0) {
                Operation op = job.operations.get(0);
                int prevOpEndTime = getPrevOperationEndTime(job.id, schedule);
                int machineAvailableTime = machines.get(op.machineId).availableTime;
                int startTime = Math.max(prevOpEndTime, machineAvailableTime);
                op.startTime = startTime;
                op.endTime = startTime + op.processingTime;

                // 临时安排工序
                schedule.add(op);
                machines.get(op.machineId).availableTime = op.endTime;
                job.operations.remove(0);

                backtrack(schedule);

                // 回溯
                schedule.remove(schedule.size() - 1);
                machines.get(op.machineId).availableTime = machineAvailableTime;
                job.operations.add(0, op);
            }
        }
    }

    static int getPrevOperationEndTime(int jobId, List<Operation> schedule) {
        int maxEndTime = 0;
        for (Operation op : schedule) {
            if (op.jobId == jobId && op.endTime > maxEndTime) {
                maxEndTime = op.endTime;
            }
        }
        return maxEndTime;
    }

    static int calculateMakespan(List<Operation> schedule) {
        int makespan = 0;
        for (Operation op : schedule) {
            if (op.endTime > makespan) {
                makespan = op.endTime;
            }
        }
        return makespan;
    }

    static void generateGanttChart(List<Operation> schedule) throws Exception {
        TaskSeriesCollection dataset = new TaskSeriesCollection();
        Map<Integer, TaskSeries> machineSeriesMap = new HashMap<>();

        // 定义基准日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(2020, Calendar.JANUARY, 1, 0, 0, 0);
        Date baseDate = calendar.getTime();

        for (Operation op : schedule) {
            TaskSeries series = machineSeriesMap.get(op.machineId);
            if (series == null) {
                series = new TaskSeries("机器 " + op.machineId);
                machineSeriesMap.put(op.machineId, series);
            }

            // 将整数时间映射为日期（假设每个时间单位为一天）
            Date startDate = addDays(baseDate, op.startTime);
            Date endDate = addDays(baseDate, op.endTime);

            // 创建任务
            Task task = new Task("作业" + op.jobId + " 工序" + op.operationId,
                    new SimpleTimePeriod(startDate, endDate));
            series.add(task);
        }

        for (TaskSeries series : machineSeriesMap.values()) {
            dataset.add(series);
        }

        JFreeChart chart = ChartFactory.createGanttChart(
                "作业车间调度甘特图",
                "机器",
                "时间",
                dataset,
                true,
                true,
                false
        );

        // 美化图表
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        GanttRenderer renderer = (GanttRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);

        // 设置作业颜色
        Paint[] colors = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN};
        int colorIndex = 0;
        for (int i = 0; i < dataset.getRowCount(); i++) {
            renderer.setSeriesPaint(i, colors[colorIndex % colors.length]);
            colorIndex++;
        }

        // 设置时间轴为整数（天）
        // 由于使用了Date对象，DateAxis会自动处理，但可以优化日期格式显示
        plot.getRangeAxis().setStandardTickUnits(org.jfree.chart.axis.DateAxis.createStandardDateTickUnits());

        // 调整字体
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.PLAIN, 12));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 10));

        // 设置背景颜色
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // 保留任务条上的数字
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator());

        // 设置任务条高度
        renderer.setMaximumBarWidth(0.1);

        // 保存甘特图为文件
        ChartUtils.saveChartAsPNG(new File("GanttChart.png"), chart, 1000, 600);
        System.out.println("甘特图已生成：GanttChart.png");
    }

    // 辅助函数：添加天数到日期
    static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }
}
