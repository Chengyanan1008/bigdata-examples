import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * 通过使用flink cep进行网站的监控报警和恢复通知
 */
public class WebMonitorAlert{

	public static void main(String[] args) throws Exception{
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		DataStream ds = env.addSource(new MySource());
		StreamTableEnvironment tenv = StreamTableEnvironment.create(env);
		tenv.registerDataStream(
				"log",
				ds,
				"traceid,timestamp,status,restime,proctime.proctime");

		String sql = "select pv,errorcount,round(CAST(errorcount AS DOUBLE)/pv,2) as errorRate," +
		             "(starttime + interval '8' hour ) as stime," +
		             "(endtime + interval '8' hour ) as etime  " +
		             "from (select count(*) as pv," +
		             "sum(case when status = 200 then 0 else 1 end) as errorcount, " +
		             "HOP_START(proctime,INTERVAL '10' SECOND,INTERVAL '5' MINUTE)  as starttime," +
		             "HOP_end(proctime,INTERVAL '10' SECOND,INTERVAL '5' MINUTE)  as endtime  " +
		             "from log  group by HOP(proctime,INTERVAL '10' SECOND, INTERVAL '5' MINUTE) )";

		Table table = tenv.sqlQuery(sql);
		DataStream<Result> ds1 = tenv.toAppendStream(table, Result.class);

		ds1.print();

		env.execute("Flink CEP web alert");
	}

	public static class MySource implements SourceFunction<Tuple4<String,Long,Integer,Integer>>{

		static int status[] = {200, 404, 500, 501, 301};

		@Override
		public void run(SourceContext<Tuple4<String,Long,Integer,Integer>> sourceContext) throws Exception{
			while (true){
				Thread.sleep((int) (Math.random() * 100));
				// traceid,timestamp,status,response time

				Tuple4 log = Tuple4.of(
						UUID.randomUUID().toString(),
						System.currentTimeMillis(),
						status[(int) (Math.random() * 4)],
						(int) (Math.random() * 100));

				sourceContext.collect(log);
			}
		}

		@Override
		public void cancel(){

		}
	}

	public static class Result{
		private long pv;
		private int errorcount;
		private double errorRate;
		private Timestamp stime;
		private Timestamp etime;

		public long getPv(){
			return pv;
		}

		public void setPv(long pv){
			this.pv = pv;
		}

		public int getErrorcount(){
			return errorcount;
		}

		public void setErrorcount(int errorcount){
			this.errorcount = errorcount;
		}

		public double getErrorRate(){
			return errorRate;
		}

		public void setErrorRate(double errorRate){
			this.errorRate = errorRate;
		}

		public Timestamp getStime(){
			return stime;
		}

		public void setStime(Timestamp stime){
			this.stime = stime;
		}

		public Timestamp getEtime(){
			return etime;
		}

		public void setEtime(Timestamp etime){
			this.etime = etime;
		}

		@Override
		public String toString(){
			return "Result{" +
			       "pv=" + pv +
			       ", errorcount=" + errorcount +
			       ", errorRate=" + errorRate +
			       ", stime=" + stime +
			       ", etime=" + etime +
			       '}';
		}
	}

}
