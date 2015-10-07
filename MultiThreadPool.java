package MultiThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

import FrameworkManagement.FrameworkManager;

import com.starbase.caliber.*;
import com.starbase.caliber.Session;
import com.starbase.caliber.server.CaliberServer;
import com.versant.fund.AttrInt;
import com.versant.fund.AttrString;
import com.versant.fund.Capability;
import com.versant.fund.ClassHandle;
import com.versant.fund.Constants;
import com.versant.fund.Handle;
import com.versant.fund.HandleEnumeration;
import com.versant.fund.HandleVector;
import com.versant.fund.NewSessionCapability;
import com.versant.fund.Predicate;
import com.versant.trans.TransSession;

import java.util.Properties;

import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.internet.InternetAddress;



public class MultiThreadPool {
	static Session session = null;
	static FrameworkManager myFrame;
	static Connection con = null;
	static TransSession session2;
	static Capability cap =null;
	static String cmdline ="";
	static String CURRENTONLY="";
	static int threadfailwait=0;
	static FileOutputStream out; // declare a file output object
	static PrintStream pio; // declare a print stream object
	static int extractsfinished=0;
	static List<String> processTest=  Collections.synchronizedList(new ArrayList<String>());
	static List<Integer> projectsrunning=  Collections.synchronizedList(new ArrayList<Integer>());
	static List<String> processList=  Collections.synchronizedList(new ArrayList<String>());
	static List<String> processList2=new ArrayList<String>();
	static int processlistsize=0;
	static String filesummary="";
	static String extractTime="";
	public static void main(String[] args) {

		try
		{

			CaliberServer server = new CaliberServer(args[6]); 
			session = server.login(args[7], args[8],"GetCheckPointDelta"); 
			String productversion = server.getServerProductVersion()+" ("+server.getServerFileVersion() + ")";

			int number_of_threads=Integer.parseInt(args[2]);
			CURRENTONLY=args[3];
			threadfailwait=Integer.parseInt(args[4])*60 *1000;
			System.out.println("If threads hang, next set will be started after " + threadfailwait + " milliseconds");
			long strtepoch= System.currentTimeMillis();
			String startTime = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(System.currentTimeMillis()));

			String temp = startTime.replaceAll("/", "-");
			temp =temp .replaceAll(":", "-");
			temp =temp .replaceAll(" ", "_");
			out = new FileOutputStream(args[11]+ "\\" + "ExtractLog" + temp + ".log");
			filesummary=args[11]+ "\\" + "ExtractLog" + temp + ".log";
			System.out.println("Started: "+startTime);
			pio = new PrintStream( out );
			System.out.println("Product Version: "+productversion);
			pio.println("Product Version: "+productversion);
			pio.println("Started: "+startTime);
			String connectionUrl = args[0];
			//get epoch time from SQL Server
			try {
				// Establish the connection.
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				con = DriverManager.getConnection(connectionUrl);
				System.out.println ("Connected");
				pio.println ("Connected");
			}
			catch(Exception ex){System.out.println ("Connection issue" + ex.getMessage());}

			Statement stmt = con.createStatement();
			Statement stmt2 = con.createStatement();
			Statement stmt3 = con.createStatement();

			String SQLinsert3 = "Select Epoch_Time FROM Extract_Info ORDER BY Epoch_Time";
			ResultSet rs=stmt.executeQuery(SQLinsert3);
			long epoch=0;
			while (rs.next()) 
			{
				epoch= rs.getInt(1);
				System.out.println(epoch);
				//System.out.println(epochnum);
				rs.next();
			}
			rs.close();

			extractTime = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date(epoch*1000));
			System.out.println(epoch);
			pio.println(epoch);
			System.out.println("Extract_Time " + extractTime);
			pio.println("Extract_Time " + extractTime);
			String DB =args[1];


			ArrayList<String> reqList= new ArrayList<String>();
			int no_of_records=0;
			Properties prop = new Properties();
			prop.put("database", DB);

			prop.put("userName", "caliber_vuser");

			prop.put("userPassword", "caliber_vuser");
			prop.put ("lockmode", Constants.NOLOCK + "");
			cap = new NewSessionCapability ();
			session2 = new TransSession(prop,cap);

			//get project id for project names
			ClassHandle clsproj = session2.locateClass ("Project_p");
			clsproj.withThisClassLockmode(Constants.NOLOCK );
			clsproj.withThisClassLockmode(Constants.NOLOCK );
			AttrInt projsid = session2.newAttrInt("Project_p::id_number");
			Scanner scanner2 = new Scanner(new FileInputStream(args[5]));
			ArrayList<String> excludeList = new ArrayList<String>();
			excludeList.clear();
			while (scanner2.hasNextLine())
			{
				String tempstr= scanner2.nextLine();
				System.out.println("Excluded Project: " + tempstr);
				pio.println("Excluded Project: " + tempstr);
				String query = tempstr;

				query =query.replaceAll("([^0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z])", "?");

				query=LocaliseString(query);
				Predicate p=session2.newAttrString("Project_p::name").matches(query);
				HandleEnumeration e = clsproj.select (p);
				HandleVector vector = session2.newHandleVector (e);

				for (int i = 0; i < vector.size(); i++) 
				{

					Handle handle = vector.handleAt (i);
					int theprojid = handle.get(projsid);
					excludeList.add(theprojid+"");
				}
			}

			myFrame = session.getRemoteFrameworkManager();
			if(args[9].equalsIgnoreCase("FirstTime")&&(extractTime.contains("1970")))
			{
				System.out.println("First Time ," + extractTime);
				pio.println("First Time ," + extractTime);
				Any rv = ORB.init().create_any();
				rv.insert_string("Project_p::id_number > 0");
				FrameworkManagement.ProjectInfo[] projects_info = myFrame.getProjectInfo(rv);



				System.out.println("No of Projects: " +projects_info.length);
				pio.println("No of Projects: " +projects_info.length);
				for(int p=0; p<projects_info.length;p++)
				{

					//System.out.println("Processing Project: " + projects_info[p].name);

					int projid_number=projects_info[p].id_number;
					FrameworkManagement.BaselineInfo[] mybasestuff = projects_info[p].baselines;
					if(!args[3].equalsIgnoreCase("currentonly"))
					{
						for(int b=0; b<mybasestuff.length;b++)
						{
							if(!mybasestuff[b].name.equalsIgnoreCase("Deleted View"))
							{
								int found = excludeList.indexOf(projid_number+"");
								if(found<0)
								{
									processList.add(projid_number+","+  mybasestuff[b].id_number);
									processList2.add(projid_number+","+  mybasestuff[b].id_number);
								}

							}
						}
					}
					else
					{
						int found = excludeList.indexOf(projid_number+"");
						if(found<0)
						{
							processList.add(projid_number+","+  projects_info[p].baselines[0].id_number);
							processList2.add(projid_number+","+  projects_info[p].baselines[0].id_number);
						}
					}
				}

			}
			else
			{

				AttrInt projid = session2.newAttrInt("CheckpointRecord_p::project_id");
				AttrInt baseid = session2.newAttrInt("CheckpointRecord_p::baseline_id");
				AttrInt reqid = session2.newAttrInt("CheckpointRecord_p::id_number");
				AttrString basename = session2.newAttrString("Baseline_p::name");
				AttrInt change = session2.newAttrInt("CheckpointRecord_p::change_type");
				AttrInt epochversant = session2.newAttrInt("CheckpointRecord_p::checkpoint_utc_time");
				Object newint = new BigDecimal(epoch).intValueExact();
				int intepoch = new BigDecimal(epoch).intValueExact();
				System.out.println("--->"+intepoch);
				pio.println("--->"+intepoch);
				Predicate r = session2.newAttrInt("CheckpointRecord_p::checkpoint_utc_time").asDate().ge((int)epoch);

				ClassHandle cls = session2.locateClass ("CheckpointRecord_p");
				ClassHandle cls3 = session2.locateClass ("RequirementProject_v300_p");
				cls.withThisClassLockmode(Constants.NOLOCK );
				cls3.withThisClassLockmode(Constants.NOLOCK );
				HandleEnumeration e = cls.select (r);
				HandleVector vector = session2.newHandleVector (e);
				System.out.println("No.of records to query"+ vector.size());
				pio.println("No.of Versant objects to query"+ vector.size());
				for (int i = 0; i < vector.size(); i++) 
				{

					no_of_records++;
					Handle handle = vector.handleAt (i);
					int theprojid = handle.get(projid);
					int thebaseid = handle.get(baseid);


					//processList.add(theprojid+","+ thebaseid);

					int requirementid = handle.get(reqid);
					int reqchange = handle.get(change);
					int epochtime = handle.get(epochversant);
					//need to check if current
					Predicate r2 = session2.newAttrInt("Baseline_p::id_number").eq(thebaseid);
					ClassHandle cls2 = session2.locateClass ("Baseline_p");
					cls2.withThisClassLockmode(Constants.NOLOCK );
					HandleEnumeration e2 = cls2.select (r2);
					HandleVector vector2 = session2.newHandleVector (e2);
					for (int i2 = 0; i2 < vector2.size(); i2++) 
					{
						Handle handle2 = vector2.handleAt (i2);
						String baselineName = handle2.get(basename);
						if(baselineName.equalsIgnoreCase("Current Baseline"))
						{
							//need to check if it exists
							//if type = 2 remove from list
							if(CURRENTONLY.equalsIgnoreCase("currentonly"))
							{
								//need to check if exists in RequirementProject_v300_p
								Predicate r3 = session2.newAttrInt("RequirementProject_v300_p::id_number").eq(theprojid);
								HandleEnumeration e3 = cls3.select (r3);
								if(e3.asArray().length>0)
								{
									int found = excludeList.indexOf(theprojid+"");
									if(found<0)
									{
										processList.add(theprojid+","+ thebaseid);
										processList2.add(theprojid+","+ thebaseid);
									}
								}
							}
							if(reqchange==2)
							{
								//deleted requirements only required for current baseline 
								reqList.add(theprojid+","+ thebaseid+","+requirementid + "," + epochtime);
							}

						}

						if(!(CURRENTONLY.equalsIgnoreCase("currentonly")))

						{
							int found = excludeList.indexOf(theprojid+"");
							if(found<0)
							{
								processList.add(theprojid+","+ thebaseid);
								processList2.add(theprojid+","+ thebaseid);
							}

						}
					}
					session2.flush();

				}
			}

			System.out.println("Length with duplicates " + processList.size());
			pio.println("Length with duplicates " + processList.size());
			HashSet<String> hashPL = new HashSet<String>(processList);
			processList.clear();
			processList.addAll(hashPL);
			HashSet<String> hashPL2 = new HashSet<String>(processList2);
			processList2.clear();
			processList2.addAll(hashPL2);
			processlistsize=processList.size();
			processTest=processList;
			System.out.println("Length without duplicates " + processList.size());
			pio.println("Length without duplicates " + processList.size());
			Collections.shuffle(processList);

			System.out.println(processList);
			System.out.println(epoch);
			pio.println(processList);
			pio.println(epoch);
			long strt=System.currentTimeMillis();
			//Collections.shuffle(processList);

			//create a record of requirements that have been deleted 
			HashSet<String> hashReq = new HashSet<String>(reqList);
			reqList.clear();
			reqList.addAll(hashReq);
			//As there are no records of the requirement for the first extract, ignore this code 
			if(epoch !=0)
			{
				for (int i = 0; i < reqList.size(); i++) 
				{
					String reqstr = reqList.get(i);
					String[] splitstr = reqstr.split(",");
					int projid =Integer.parseInt(splitstr[0]);
					int baseid =Integer.parseInt(splitstr[1]);
					int reqid =Integer.parseInt(splitstr[2]);
					long epochnum=Integer.parseInt(splitstr[3]);
					String date = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epochnum*1000));

					//If a requirement has been created and deleted prior to an extract the type, tree version etc will not be known
					//The user that has deleted the requirement is unknown as Caliber does not record this
					String SQLselect = "Select TOP 1 Req_ID,Type, Version,no_changes FROM Tree WHERE Req_ID="+ reqid + " ORDER BY Req_ID DESC ";
					ResultSet result = stmt.executeQuery(SQLselect);
					String reqtype ="Not Known";
					String treeversion ="Not Known";
					int  no_chgs =-1;
					while(result.next())
					{
						reqtype = result.getString("Type");
						treeversion = result.getString("Version");
						no_chgs = result.getInt("no_changes");

					}
					try
					{
						String[] delversionspli = treeversion.split("[.]");
						int intdelthing = Integer.parseInt(delversionspli[0]);
						intdelthing=intdelthing+ 1;
						treeversion=intdelthing+".0";
					}
					catch(Exception e){}
					String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projid+","+ baseid+",'"+ LocaliseString(reqtype)+"'"+",-1,"+reqid + ",'" + treeversion + "'," + "3" +",'Deleted',-1,'','',2,'"+date+"')";

					stmt.execute(SQLinsert);

				}
			}

			System.out.println("Number of Project Baselines: "+processList.size());
			pio.println("Number of Project Baselines: "+ processList.size());
			//Open Master file to obtain java run string
			String Master = null;

			Scanner scanner = new Scanner(new FileInputStream(args[12]));

			while (scanner.hasNextLine())
			{
				Master=scanner.nextLine();
			}

			String temp1 ="";

			int count =0;



			//Obtain max number of threads to create 
			int no_of_threads = Integer.parseInt(args[2]);
			ExecutorService executor = Executors.newFixedThreadPool(no_of_threads);


			//A project baselines cannot run if an extraction of the same project is running.
			//This is to prevent SQL deadlock due to the fact the application requires to query if the req_info for a requirement version exists
			//If it does not exist it will be created. So if a project has multiple baseline changes and is multiple threads 

			while(!processList.isEmpty())
			{


				for (int i = 0; i < processList.size(); i++) 
				{
					try
					{
					temp1=processList.get(i);
					String[] temp2 = temp1.split("[,]");



					cmdline = Master.replace("projID", temp2[0]);
					cmdline = cmdline.replace("baseID", temp2[1]);
					int proid=Integer.parseInt(temp2[0]);
					//System.out.println("info: " +temp1 +"  " + projectsrunning + "  " + proid+ " " + projectsrunning.indexOf(proid));
					Collections.synchronizedCollection(projectsrunning);
					if(!projectsrunning.contains(proid))
					{
						System.out.println("Start Thread " +temp1);
						long currentepoch = System.currentTimeMillis()/1000;
						String date = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (currentepoch*1000));
						//pio.println("Start Thread " +temp1 + " " + date);

						projectsrunning.add(proid);
						Collections.synchronizedCollection(projectsrunning);
						runstuff(cmdline,executor);
						System.out.println("Thread");
					}
					/*else
				{
					if(!processList.isEmpty())
					{
						int lastindex=processList.size()-1;
						processList.set(lastindex, temp1);
						Collections.synchronizedCollection(processList);
					}
				}*/
					//System.out.println("---> "+projectsrunning);

					//Runnable worker = new WorkerThread(cmdline);
					}
					catch(Exception ex)
					{
						System.out.println("processList split issue!!!");
					}

				}
			}
			executor.shutdown();
			while (!executor.isTerminated()) {
			}
			System.out.println("Finished all threads");
			pio.println("Finished all threads");

			long finepoch= System.currentTimeMillis();
			String finTime = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(finepoch));
			System.out.println("Finished monitoring threads: "+finTime);
			System.out.println("");
			pio.println("Finished monitoring threads: "+finTime);
			pio.println("");
			//UPDATE extraction time
			String SQLinsert = "DELETE FROM Extract_Info";
			stmt.execute(SQLinsert);
			SQLinsert = "INSERT INTO Extract_Info  VALUES (" +strtepoch/1000+ ")";
			stmt.execute(SQLinsert);


			String Dups=args[10];
			if(Dups.equalsIgnoreCase("RemoveDups"))
			{


				System.out.println("Cleaning up any duplicates");
				pio.println("Cleaning up any duplicates");
				System.out.println("--------------------------");
				//remove duplicates caused by multi threading and batch sql execute
				try
				{
					stmt.execute("Alter TABLE Project_INFO ADD RowId int not null identity(1,1) primary key");
				}
				catch(Exception ex){}

				try
				{
					stmt.execute("DELETE Project_INFO FROM Project_INFO LEFT OUTER JOIN (SELECT MIN(RowId) as RowId, ProjectID, Name FROM Project_INFO  GROUP BY ProjectID, Name) as KeepRows ON Project_INFO.RowId = KeepRows.RowId WHERE KeepRows.RowId IS NULL");
				}
				catch(Exception ex){}

				System.out.println("Cleaned duplicate Projects");
				pio.println("Cleaned duplicate projects");

				String SQLalter="Alter TABLE Baseline_INFO ADD RowId int not null identity(1,1) primary key";
				try
				{
					stmt.execute(SQLalter);
				}
				catch(Exception ex){}
				String SQLremovedups="DELETE Baseline_INFO FROM Baseline_INFO LEFT OUTER JOIN (SELECT MIN(RowId) as RowId, BaselineID, TypeName FROM Baseline_INFO GROUP BY BaselineID, TypeName) as KeepRows ON Baseline_INFO.RowId = KeepRows.RowId WHERE KeepRows.RowId IS NULL";
				stmt.execute(SQLremovedups);

				System.out.println("Cleaned duplicate Baselines");
				pio.println("Cleaned duplicate Baselines");

				SQLalter="Alter TABLE [REQ_INFO] ADD RowId int not null identity(1,1) primary key";
				try
				{
					stmt.execute(SQLalter);
				}
				catch(Exception ex){}

				SQLremovedups="DELETE [REQ_INFO] FROM [REQ_INFO] LEFT OUTER JOIN (SELECT MIN(RowId) as RowId, Req_ID, Version, UDAName, UDAValue FROM [REQ_INFO] GROUP BY Req_ID, Version, UDAName, UDAValue) as KeepRows ON [REQ_INFO].RowId = KeepRows.RowId WHERE KeepRows.RowId IS NULL";
				stmt.execute(SQLremovedups);

				System.out.println("Cleaned duplicate Requirement info");
				pio.println("Cleaned duplicate Requirement info");

				SQLalter="Alter TABLE [Trace_Info] ADD RowId int not null identity(1,1) primary key";
				try
				{
					stmt.execute(SQLalter);
				}
				catch(Exception ex){}

				SQLremovedups="DELETE [Trace_Info] FROM [Trace_Info] LEFT OUTER JOIN (SELECT MIN(RowId) as RowId, [Req_ID_From],[Version_From],[DIRECTION],[Req_ID_TO],[Version_TO] FROM [Trace_Info] GROUP BY[Req_ID_From],[Version_From],[DIRECTION],[Req_ID_TO],[Version_TO]) as KeepRows ON [Trace_Info].RowId = KeepRows.RowId WHERE KeepRows.RowId IS NULL";
				stmt.execute(SQLremovedups);

				System.out.println("Cleaned duplicate Trace info");
				pio.println("Cleaned duplicate Trace info");
			}
			else
			{
				System.out.println("Remove Duplicates not set!");
				pio.println("Remove Duplicates not set!");
			}
			System.out.println("Finished");


			pio.println("");
			System.out.println("");
			System.out.println("SUMMARY");
			System.out.println("-----------------------------------------------------------------------------");
			pio.println("SUMMARY");
			pio.println("------------------------------------------------------------------------------------");
			System.out.println("Extraction Finished: "+extractsfinished+"/"+  processlistsize + " " + processTest.toString());



			pio.println("Extraction Finished: "+extractsfinished+"/"+  processlistsize + " " + processTest.toString());
			System.out.println("Started: "+startTime);
			pio.println("Started: "+startTime);
			long finishepoch= System.currentTimeMillis();
			String finishTime = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(System.currentTimeMillis()));
			System.out.println("Finished: "+finishTime);
			pio.println("Finished: "+finishTime);
			long overall = finishepoch-strtepoch;
			long no_mins = overall / 1000;
			System.out.println("Overall Seconds: "+no_mins);
			pio.println("Overall Seconds: "+no_mins);
			pio.println("------------------------------------------------------------------------------------");
			System.out.println("-----------------------------------------------------------------------------");
			pio.println(args[14]);
			pio.close();
			session.logout();
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
		}
		//email contents of file.
		String passed="";
		String flag="";
		if(extractsfinished==processlistsize)
		{
			passed="Extraction Completed Successfully: " +  extractsfinished + " / " + processlistsize;
			flag="passed";
		}
		else
		{
			passed="Extraction failed:" +  extractsfinished + " / " + processlistsize;
			flag="failed";
		}


		try 
		{
			System.out.println(args[14]);
			if(args[14].equalsIgnoreCase("EMAIL"))
			{
			//build results from file
			String HTML="<HTML>";
			FileInputStream fstream = new FileInputStream(filesummary);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;

			//Read File Line By Line
			while ((strLine = br.readLine()) != null)   
			{
				// Print the content on the console
				HTML=HTML+ strLine+ "<br>";
				//System.out.println (strLine);
			}
			HTML=HTML+"</HTML>";
			//Close the input stream
			br.close();
			//Open email config file
			String emailhost="";
			String username="";
			String userpassword="";
			String emailport="";
			String emaillist="";
			String emailTiltle="";
			String emailauthentication="";
			String emailFrom="";
			try
			{
				//Scanner scanneremail = new Scanner(new FileInputStream(args[13]));

				FileInputStream fstream2 = new FileInputStream(args[13]);
				BufferedReader br2 = new BufferedReader(new InputStreamReader(fstream2));

				emailhost=br2.readLine();
				//System.out.println(emailhost);
				username=br2.readLine();
				//System.out.println(username);
				userpassword=br2.readLine();
				//System.out.println("xxxxxxx");
				emailport=br2.readLine();
				//System.out.println(emailport);
				emaillist=br2.readLine();
				//System.out.println(emaillist);
				emailTiltle=br2.readLine();
				//System.out.println(emailTiltle);
				emailauthentication=br2.readLine();
				//System.out.println(emailauthentication);
				emailFrom=br2.readLine();
				//System.out.println(emailFrom);
			}
			catch(Exception e)
			{
				System.out.println("email read file: "+e.getMessage());
			}

			String summaryhtml="<html>"
					+"<style>"
					+"table, th, td {"
					+"    border: 1px solid black;"
					+"    border-collapse: collapse;"
					+"}"
					+"</style>"
					+"<p>"
					+"</p>"
					+"<p>"
					+"<h2>Extraction Requirement Change Summary</h2>"
					+"</p>"
					+"<table border =1 style=\"width:100%\">"
					+"  <tr>"
					+"    <th>Project</th>"
					+"    <th>Baseline</th>" 
					+"    <th>No. Reqs Before</th>"
					+"	<th>No. Reqs Total</th>"
					+"	<th>Total Updated</th>"
					+"	<th>Total New</th>"
					+"	<th>% Unchanged</th>"
					+"	<th>% Created</th>"
					+"	<th>Extraction Date</th>"
					+"  </tr>";
			int updatecount=0;
			Collections.sort(processList2);
			for (int i = 0; i < processList2.size(); i++) 
			{
				String temp1 = processList2.get(i);
				String[] temp2 = temp1.split("[,]");

				int baselineid=Integer.parseInt(temp2[1]);
				String time=extractTime.replace("/", "-");

				String SQL="Declare @baseID int;"
						+"Declare @InitialDate VARCHAR(32);"
						+"Set @baseID = "+baselineid+";"
						+"SET @InitialDate='"+time+"';"
						+" SELECT"
						+"(SELECT DISTINCT ProjectName FROM Baseline_INFO WHERE BaselineID=@baseID) Project,"
						+"(SELECT DISTINCT Name FROM Baseline_INFO WHERE BaselineID=@baseID) Baseline,"
						+"(SELECT COUNT(*) FROM Tree"
						+" WHERE Baseline_ID=@baseID"
						+" AND creationDate <@InitialDate) NoReqsBefore," 
						+"(SELECT COUNT(*) FROM Tree"
						+" WHERE Baseline_ID=@baseID) TotalnoReqs,"
						+" (SELECT COUNT(*) FROM Tree"
						+" WHERE Baseline_ID=@baseID AND moddate >@InitialDate) TotalUpdated,"
						+" (CAST(((CAST((SELECT COUNT(*) as Percentagechanged FROM Tree WHERE Baseline_ID=@baseID"
						+" AND modDate <=@InitialDate)as float) *100) / (SELECT COUNT(*) FROM Tree"
						+" WHERE Baseline_ID=@baseID))as numeric(10,2))) as Percentageunchanged,"
						+" (CAST(((CAST((SELECT COUNT(*) as Percentagechanged FROM Tree WHERE Baseline_ID=@baseID"
						+" AND creationDate >=@InitialDate)as float) *100) / (SELECT COUNT(*) FROM Tree"
						+" WHERE Baseline_ID=@baseID))as numeric(10,2))) as Percentagechanged,"
						+" (SELECT COUNT(*) FROM Tree"
						+" WHERE Baseline_ID=@baseID AND creationDate >@InitialDate) TotalNew,"
						+" (SELECT SUM(no_changes)  FROM Tree"
						+" WHERE Baseline_ID=@baseID AND moddate >@InitialDate) SinceLastExtract,"
						+"(SELECT  @InitialDate ) ExtractDate";


				//System.out.println(SQL);
				Statement stmt = con.createStatement();
				ResultSet rs=stmt.executeQuery(SQL);

				summaryhtml=summaryhtml+ "<tr>";

				while (rs.next()) 
				{
					String test= rs.getString("Project");
					String test2= rs.getString("Baseline");
					String test3= rs.getString("NoReqsBefore");
					String test4= rs.getString("TotalnoReqs");
					String test5= rs.getString("Percentageunchanged");
					String test6= rs.getString("Percentagechanged");
					String test7= rs.getString("TotalNew");
					String test8= rs.getString("SinceLastExtract");
					String test9= rs.getString("ExtractDate");
					String test10= rs.getString("TotalUpdated");
					
						summaryhtml=summaryhtml+ "<td>"+test + "</td>";
						summaryhtml=summaryhtml+ "<td>"+test2 + "</td>";
						if(test2.equalsIgnoreCase("Current Baseline"))
						{
							summaryhtml=summaryhtml+ "<td align=right>"+test3 + "</td>";
							summaryhtml=summaryhtml+ "<td align=right>"+test4 + "</td>";
							summaryhtml=summaryhtml+ "<td align=right>"+test10 + "</td>";
							summaryhtml=summaryhtml+ "<td align=right>"+test7 + "</td>";
							summaryhtml=summaryhtml+ "<td align=right>"+test5 + "</td>";
							summaryhtml=summaryhtml+ "<td align=right>"+test6 + "</td>";
							//summaryhtml=summaryhtml+ "<td align=right>"+test8 + "</td>";
						}
						else
						{
							summaryhtml=summaryhtml+ "<td align=right>N/A</td>";
							summaryhtml=summaryhtml+ "<td align=right>"+test4 + "</td>";
							summaryhtml=summaryhtml+ "<td align=right>N/A</td>";
							summaryhtml=summaryhtml+ "<td align=right>N/A</td>";
							summaryhtml=summaryhtml+ "<td align=right>N/A</td>";
							summaryhtml=summaryhtml+ "<td align=right>N/A</td>";
						}
						summaryhtml=summaryhtml+ "<td align=right>"+test9 + "</td>";
						rs.next();
						updatecount++;
					
					
				}



				rs.close();

			}
			summaryhtml=summaryhtml+ "</tr></table><br><br>";
			if(updatecount!=0)
			{
				HTML =HTML.replaceAll("<HTML>", summaryhtml);
			}
			List<String> toList = new ArrayList();
			if(emaillist.contains(","))
			{
				String[] temp = emaillist.split(",");
				for (int i = 0; i < temp.length; i++) 
				{
					toList.add(i, temp[i]);
				}
			}
			else
			{
				toList.add(0, emaillist);
			}

			boolean emailauthent=false;
			if(emailauthentication.equalsIgnoreCase("true"))
			{
				emailauthent=true;
			}
			con.close();
			
			sendMailWithAuth(emailhost, username, userpassword, emailport, toList, HTML, emailTiltle + passed,flag,emailauthent,emailFrom);
			}
			//sendMailWithAuth("smtp.live.com", "user@hotmail.co.uk", "password", "25", toList, HTML, "Caliber Extraction: " + passed,flag,true,"david.carson@microfocus.com");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	private static Future<?> runstuff(final String cmdline,final ExecutorService exec) {
		// TODO Auto-generated method stub


		Future<?> task = exec.submit(new Runnable() {
			@Override
			public void run() 
			{
				BufferedReader is;
				String line;
				try
				{

					//change user and password
					//System.out.println("Running: "+cmdline);
					Process pr = Runtime.getRuntime().exec(cmdline);
					is= new BufferedReader(new InputStreamReader(pr.getInputStream()));
					while((line=is.readLine())!=null)
					{
						System.out.println(line);
						if(line.contains("FINISHED-->"))
						{
							extractsfinished++;
							//parse out project baselines
							int pos = line.indexOf("~");
							int pos2 = line.indexOf("~",pos+1);
							String projectid=line.substring(pos+1, pos2);
							pos = line.indexOf("~", pos2+1);
							pos2 = line.indexOf("~",pos+1);
							String Baselineid=line.substring(pos+1, pos2);
							processTest.remove(projectid + "," + Baselineid);
							Collections.synchronizedCollection(processList);
							processList.remove(projectid + "," + Baselineid);
							long currentepoch = System.currentTimeMillis()/1000;
							String date = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (currentepoch*1000));
							pio.println("["+extractsfinished+"] "+ line + " " + date);
							int pid=Integer.parseInt(projectid);
							Collections.synchronizedCollection(projectsrunning);
							System.out.println(projectsrunning);
							synchronized(projectsrunning) {
								Iterator it=projectsrunning.iterator(); 
								while(it.hasNext()) { 
									System.out.println(it.next()); 
								} 
							}
							//projectsrunning=new CopyOnWriteArrayList();
							System.out.println(pid);
							int index = projectsrunning.indexOf(pid);
							if(index >=0)
							{
								projectsrunning.remove(index);
							}
							System.out.println("<-- "+projectsrunning);



						}
					}
					pr.waitFor();
				}

				catch(Exception rex){rex.printStackTrace();;}
			}
		});
		return task;

	}

	public static String LocaliseString(String Value)
	{

		try
		{
			//need to remove hex characters etc	
			StringBuffer tempBuffer = new StringBuffer();
			int incrementor = 0;
			int dataLength = Value.length();
			while (incrementor < dataLength) {
				char charecterAt = Value.charAt(incrementor);
				if (charecterAt == '%') {
					tempBuffer.append("<percentage>");
				} else if (charecterAt == '+') {
					tempBuffer.append("<plus>");
				} else {
					tempBuffer.append(charecterAt);
				}
				incrementor++;
			}
			Value = tempBuffer.toString();
			Value = URLDecoder.decode(new String(Value.getBytes("ISO-8859-1"), "UTF-8"), "UTF-8");
			Value = Value.replaceAll("<percentage>", "%");
			Value = Value.replaceAll("<plus>", "+");	


		}
		catch(Exception ex)
		{
			System.out.println("Encode error "+ ex.getMessage());
		}
		Value = Value.replaceAll("'", "''");
		//Value = Value.replaceAll("�", "��");
		Value=escapeHTML(Value);

		return Value;
	}

	public static String escapeHTML(String html)
	{

		String str =html;

		StringBuilder strbuild = new StringBuilder(); 
		for (int i=0; i<str.length();i++)
		{
			//System.out.println(str.charAt(i) + " as ASCII value");
			char result = str.charAt(i);
			if(result>255)
			{
				int result2 =str.charAt(i);;
				strbuild.append("&#" + result2 + ";");
			}
			else
			{
				strbuild.append(result);
			}


		}

		html=strbuild.toString();
		//html=URLDecoder.decode(html);
		return html;

	}

	public static void sendMailWithAuth(String host, String user, String password, String port, List<String> toList,
			String htmlBody, String subject, String fail,boolean author, String Fromemail) throws Exception {

		Properties props = System.getProperties();

		props.put("mail.smtp.user",user); 
		props.put("mail.smtp.password", password);
		props.put("mail.smtp.host", host); 
		props.put("mail.smtp.port", port); 
		//props.put("mail.debug", "true"); 
		props.put("mail.smtp.auth", "false"); 
		props.put("mail.smtp.starttls.enable","true"); 
		props.put("mail.smtp.EnableSSL.enable","true");
		//props.put("mail.smtp.auth.plain.disable", true);





		javax.mail.Session session = javax.mail.Session.getInstance(props, null);
		//session.setDebug(true);

		javax.mail.internet.MimeMessage message = new javax.mail.internet.MimeMessage(session);
		message.setFrom(new InternetAddress(Fromemail));

		// To get the array of addresses
		for (String to: toList) {
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		}

		message.setSubject(subject);
		message.setContent(htmlBody, "text/html");

		if(fail.equalsIgnoreCase("failed"))
		{
			//set importance flag in email if failed
			message.setHeader("X-Priority", "1");
		}
		Transport transport = session.getTransport("smtp");
		try 
		{
			if(author)
			{
				transport.connect(host, user, password);
			}
			else
			{
				transport.connect();
			}
			transport.sendMessage(message, message.getAllRecipients());
		} finally {
			transport.close();
		}
	}
}
