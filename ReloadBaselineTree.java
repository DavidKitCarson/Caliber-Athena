import com.starbase.caliber.*;
import com.starbase.caliber.server.*;
import com.starteam.Item;
import com.starteam.LinkValue;
import com.starteam.Property;
import com.starteam.PropertyCollection;
import com.starteam.Server;
import com.starteam.TraceCollection;
import com.starteam.TypeCollection;
import com.starteam.View;
import com.starteam.Item.Type;
import com.versant.fund.AttrHandleArray;
import com.versant.fund.AttrInt;
import com.versant.fund.AttrBoolean;
import com.versant.fund.AttrHandle;
import com.versant.fund.AttrString;
import com.versant.fund.ClassHandle;
import com.versant.fund.Constants;
import com.versant.fund.Handle;
import com.versant.fund.HandleEnumeration;
import com.versant.fund.HandleVector;
import com.versant.fund.NewSessionCapability;
import com.versant.fund.Predicate;
import com.versant.trans.TransSession;

import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omg.CORBA.Any;
import org.omg.CORBA.LongSeqHelper;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ShortSeqHelper;
import org.omg.CORBA.portable.InputStream;
import org.w3c.tidy.Tidy;

import sun.misc.BASE64Encoder;
import Caliber.TraceInfo;
import Caliber.Traces;
import Caliber.UDAInfo;
import Caliber.UDAMultipleSelectionListInfo;
import Caliber.UDASelectionListEntry;
import Caliber.UDASingleSelectionListInfo;
import Caliber.VersionInfo;
import Caliber.eUDAType;
import FrameworkManagement.FrameworkManager;
import FrameworkManagement.GenericObjectInfo;
import FrameworkManagement.GroupInfo;
import FrameworkManagement.ProjectInfo;
import FrameworkManagement.UserInfo;
import RequirementManagement.RequirementInfo;
import RequirementManagement.RequirementManager_v102;
import RequirementManagement.RequirementTreeItem;
import RequirementManagement.RequirementTypeInfo;
import RequirementManagement.RequirementTypeInfo_v102;

import com.versant.fund.Capability;

import java.net.URLDecoder;
import java.sql.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;



class ReloadBaselineTree {
	static FrameworkManager myFrame;
	// Declare the JDBC objects.
	static Connection con = null;
	static Statement stmt = null;
	static Statement stmtUpdate = null;
	static ResultSet rs = null;
	static FileOutputStream Errors; // declare a file output object
	static PrintStream pioErrors; // declare a print stream object
	static FileOutputStream progress; // declare a file output object
	static PrintStream pioProgress; 

	static String Hostname="";
	static String baselinename="";
	static CaliberServer server;
	static Session session = null;
	static Date startTime;
	static String starteamlogin="";
	static String starteampass="";
	static String STYES="";
	static String QCYES="";
	static long epochnum = 0;
	static String DBname="";
	static String QCDB ="";
	static String QCDBuser ="";
	static String QCDBpassword ="";
	static TransSession session2;
	static String DB ="";
	static Capability cap =null;
	static ArrayList<String> StatusArray = new ArrayList<String>();
	static ArrayList<String> PriorityArray = new ArrayList<String>();
	static String QCURL="";
	static String QCUser="";
	static String QCPassword="";
	static String CURRENTONLY="";
	static int REQCOUNT=0;
	static String errorpoint ="";
	static String strAddress="";
	static int nPort = -1;
	static int projID=-1;
	static int viewID=-1;
	static String updatetime="";
	static RequirementManager_v102 reqman = null;
	
	public static void main(String[] args) 
	{

		try
		{
			CURRENTONLY=args[6];
			STYES=args[10];
			QCYES=args[11];
			QCURL=args[12];
			QCUser=args[13];
			QCPassword=args[14];
			Hostname=args[0];
			DB=args[9];
			int projectID = Integer.parseInt(args[15]);
			int baselinesID = Integer.parseInt(args[16]);
			
			Properties prop = new Properties();
			prop.put("database", DB);

			prop.put("userName", args[1]);

			prop.put("userPassword", args[2]);
			prop.put ("lockmode", Constants.NOLOCK + "");
			cap = new NewSessionCapability ();
			session2 = new TransSession(prop,cap);



			String connectionUrl = args[4];

			try 
			{
				// Establish the connection to SQL Server.
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				con = DriverManager.getConnection(connectionUrl);
				System.out.println ("Connected");
				stmt = con.createStatement();
				
			}
			catch(Exception ex)
			{
				System.out.println ("Connection issue" + ex.getMessage());
				
			}
			
			//StarTeam and Caliber Login have to be the same
			starteamlogin=args[1];
			starteampass=args[2];
			server = new CaliberServer(args[0]);
			session = server.login( args[1], args[2],"CaliberRM Athena Delta Extractor");
			System.out.println ("login successful");
			
			reqman=session.getRemoteRequirementManager();
			myFrame = session.getRemoteFrameworkManager();

			ArrayList<Integer> UpDateList= new ArrayList<Integer>();

			//get name from caliber

			String baselinesname ="";
			AttrString basename = session2.newAttrString("Baseline_p::name");
			
			System.out.println ("Project ID:" + projectID + " Baseline ID: " + baselinesID);
			Predicate r2 = session2.newAttrInt("Baseline_p::id_number").eq(baselinesID);
			ClassHandle cls2 = session2.locateClass ("Baseline_p");
			HandleEnumeration e2 = cls2.select (r2);
			HandleVector vector2 = session2.newHandleVector (e2);
			for (int i2 = 0; i2 < vector2.size(); i2++) 
			{
				Handle handle2 = vector2.handleAt (i2);
				baselinesname = handle2.get(basename);
			}

			progress= new FileOutputStream("C:\\CaliberRM_Athena\\logs\\DeltaProgress"+projectID+"_"+baselinesID+".txt");
			pioProgress= new PrintStream(progress);
			pioProgress.println("Reloading Tree Called");
			boolean resuk = ReloadTree(UpDateList,projectID,baselinesID,session,baselinesname,args[5],reqman,myFrame);
			stmt.executeBatch();
			pioProgress.println("Reloading Tree Finished");

			pioProgress.close();
			session.getCurrentUser();

			stmt.close();

			con.close();
			
			session.logout();

			session2.flush();
			

		}
		catch(Exception rse)
		{
			System.out.println("error 1:"+rse.getMessage());
			pioProgress.println("ERROR:"+rse.getMessage() );
		}

		finally
		{
			if(session != null)
			{
				try {
					session.logout();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}



	static boolean ReloadTree(ArrayList<Integer> UpDateList,int projectID,int baselineID,Session session, String baseName,String level,RequirementManager_v102 reqman,FrameworkManager myFrame)
	{
		//Reloading baseline trees
		HashSet<Integer> h = new HashSet<Integer>(UpDateList);
		UpDateList.clear();
		UpDateList.addAll(h);
		try {
			//delete baseline
			//System.out.println(con.isClosed());

			System.out.println ("Blowing away baseline tree");
			long strt= System.currentTimeMillis();
			updatetime = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (strt));

			String SQLinsert3 = "DELETE FROM Baseline_Info WHERE BaselineID =" + baselineID;
			stmt.execute(SQLinsert3);

			errorpoint="project & baseline deletion";
			SQLinsert3 ="WHILE EXISTS (SELECT REQ_ID FROM Tree WITH(UPDLOCK , READPAST) Where Project_ID="+projectID + " AND Baseline_ID="+baselineID +") BEGIN SET ROWCOUNT 1000 DELETE FROM Tree Where Project_ID="+projectID + " AND Baseline_ID="+baselineID + " SET ROWCOUNT 0 END";
			stmt.execute(SQLinsert3);

			con.commit();
			
			long fin= System.currentTimeMillis();
			System.out.println ("Deletion took:"+(fin -strt)/1000);
			System.out.println("Deletion of tree complete");

		} catch (Exception e1) 

		{
			System.out.println("DELETE Create SQL Err:"+e1.getMessage());
		}
		int MajorNumber=0;
		int Order =0;
		int theDepth=0;
		int no_of_depth=0;
		int startcount=0;
		String myType = null;

		String hiearchy = "";

		String parentlast="";
		String[] anArray; 

		long starttime = System.currentTimeMillis();
		RequirementTreeItem[] treestuff=null;
		try{

			RequirementManager_v102 mystuff =session.getRemoteRequirementManager();
			treestuff = mystuff.getBaselineTree(projectID, baselineID, true);
			System.out.println("Processing " + treestuff.length + " Objects");
			pioProgress.println("Processing " + treestuff.length + " Objects");
		}
		catch( Exception e ) {}

		try{
			anArray = new String[treestuff.length];  

			String myproject ="";
			for(int i=0; i<treestuff.length;i++)
			{
				//System.out.println(treestuff[i].depth + "," + treestuff[i].name);
				String depth ="";
				for(int d=0; d<treestuff[i].depth;d++)
				{
					depth=depth+" ";
				}
				if (treestuff[i].depth==0)
				{

					myproject = treestuff[i].name;

					stmt.execute("DELETE FROM Project_INFO WITH(READPAST) WHERE ProjectID=" + projectID);
					//check if exists
					errorpoint="Project Query";
					String SQLCheck = "Select * From Project_INFO WITH(UPDLOCK , READPAST) WHERE ProjectID=" + projectID;
					ResultSet result=stmt.executeQuery(SQLCheck);


					boolean ProjChk=false;				
					while (result.next()) 
					{
						ProjChk=true;
					}
					result.close();
					if(ProjChk==false)
					{
						String SQLinsert3 = "INSERT INTO Project_Info VALUES (" + projectID + ",'" + LocaliseString(myproject)+ "')";
						stmt.execute(SQLinsert3);
					}
					System.out.println(LocaliseString(myproject) + "," + LocaliseString(baseName));
				}

				//requirement types are at depth = 1
				if (treestuff[i].depth==1)
				{

					myType = treestuff[i].name;
					//System.out.println(myType);
					MajorNumber=0;
					try
					{
						if(myType.equalsIgnoreCase("Templates") || myType.equalsIgnoreCase("Data")||myType.equalsIgnoreCase("Actors")||myType.equalsIgnoreCase("Images"))
						{

						}
						else
						{
							errorpoint="Baseline insert";
							String SQLinsert3 = "INSERT INTO Baseline_Info VALUES (" + baselineID + ",'" + LocaliseString(baseName)+ "','"+ LocaliseString(myproject)+"','"+ LocaliseString(myType) +"')";
							stmt.execute(SQLinsert3);
							con.commit();
						}
					}
					catch(Exception Ex){}

				}
				if ((treestuff[i].depth>1))
				{

					if (treestuff[i].depth==2)
					{
						MajorNumber++;
						hiearchy= MajorNumber+ "";
						startcount =i;

						no_of_depth=0;
					}


					if ((treestuff[i].depth>2))
					{

						theDepth = treestuff[i].depth;


						//need to find parent and count child depth
						no_of_depth=1;
						int parentdepth =0;

						//find parent
						for(int d=startcount; d<i;d++)
						{
							if (treestuff[d].depth==theDepth-1)
							{
								//found parent
								parentdepth=d;
								parentlast = anArray[d];
							}
						}

						//count children
						for(int d=parentdepth; d<i;d++)	
						{


							if (treestuff[d].depth==theDepth)
							{

								no_of_depth++;

							}


						}


						hiearchy= parentlast + "."  + no_of_depth;

					}

					Order++;
					//System.out.println(projectID+" "+ baselineID + " " + Order + " " + myType + " " + hiearchy +" "+ treestuff[i].name+ ";"+treestuff[i].object_id.caliber_object_id().id_number + ";" + treestuff[i].version_info.major_version_number + "." + treestuff[i].version_info.minor_version_number );
					String reqname = treestuff[i].name.replaceAll("(\\r|\\n)+", " ");

					reqname=reqname.replaceAll("'", "");
					try{
						String almlink="<A href=''"+"";
						//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
						if(baseName.equalsIgnoreCase("Current Baseline"))
						{

							almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ treestuff[i].object_id.caliber_object_id().id_number+ ";ns=requirement'"+"'>Req:"+treestuff[i].object_id.caliber_object_id().id_number+"</A>";

						}
						else
						{
							almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ treestuff[i].object_id.caliber_object_id().id_number+ ";ns=requirement?baseline=" + baselineID +"''>Req:"+treestuff[i].object_id.caliber_object_id().id_number+"</A>";
							//Check if the requirement info for the version exists
							//if not and tiponly create it

							for (Iterator<Integer> iter = UpDateList.iterator();iter.hasNext(); ) 
							{
								Integer reqid = iter.next();
								if (reqid==treestuff[i].object_id.caliber_object_id().id_number)
								{
									//obtain requirement version info and populate SQL DB
									//AddReqInfo(UpDateList,session,projectID,baselineID,level);
									//AddReqInfoV(reqid, treestuff[i].version_info,session,projectID,baselineID,level);


								}
							}
						}


						//int no_chgs = reqinfo[0].available_versions.length;

						//Any rvV = ORB.init().create_any();

						//need to obtain creation and mod date for requirement version
						//Query Requirement object
						errorpoint="Creation Date etc";
						int majV=treestuff[i].version_info.major_version_number;
						int minV=treestuff[i].version_info.minor_version_number;
						String creationdate=ReturnCreationDate(treestuff[i].object_id.caliber_object_id().id_number);

						String temp = ReturnModDate(treestuff[i].object_id.caliber_object_id().id_number,majV,minV);

						String[] temp2 = temp.split("[|]");

						String moddate=temp2[0];

						String no_of_revisions=temp2[1];

						errorpoint="Insert update tree \\ requirement";
						String SQLinsert = "INSERT INTO Tree VALUES (" + projectID + ","+ baselineID + "," + Order + ",'" + LocaliseString(myType) + "','" + hiearchy +"','"+ LocaliseString(reqname) + "',"+treestuff[i].object_id.caliber_object_id().id_number + ",'" + treestuff[i].version_info.major_version_number + "." + treestuff[i].version_info.minor_version_number +"'"+",'"+almlink+ "','','','','','','','',"+no_of_revisions+",'"+creationdate+"','"+moddate+"',"+treestuff[i].depth+")";
						try
						{
							stmt.execute(SQLinsert);	
						}
						catch(Exception Ex)
						{
							System.out.println("Retry SQL:" + errorpoint);
							Thread.sleep(1000);
							stmt.execute(SQLinsert);	
						}
						errorpoint="Checking Reqinfo Version";
						
						//check if reqversion exists, if not remove and update
						boolean ItExists =false;
						try
						{
							String SQLTestExist="SELECT DISTINCT Req_ID FROM REQ_INFO WITH(UPDLOCK , READPAST) WHERE Req_ID =" + treestuff[i].object_id.caliber_object_id().id_number +" and Version ='" + treestuff[i].version_info.major_version_number + "." + treestuff[i].version_info.minor_version_number+"'";

							ResultSet testresult = stmt.executeQuery(SQLTestExist);

							while (testresult.next()) 
							{
								ItExists=true;
							}
							testresult.close();
						}
						catch(Exception ex)
						{
							try
							{
								Thread.sleep(1000);
								String SQLTestExist="SELECT DISTINCT Req_ID FROM REQ_INFO WITH(UPDLOCK , READPAST) WHERE Req_ID =" + treestuff[i].object_id.caliber_object_id().id_number +" and Version ='" + treestuff[i].version_info.major_version_number + "." + treestuff[i].version_info.minor_version_number+"'";

								ResultSet testresult = stmt.executeQuery(SQLTestExist);

								while (testresult.next()) 
								{
									ItExists=true;
								}
								testresult.close();
							}
							catch(Exception ex2)
							{
								Thread.sleep(2000);
								String SQLTestExist="SELECT DISTINCT Req_ID FROM REQ_INFO WITH(UPDLOCK , READPAST) WHERE Req_ID =" + treestuff[i].object_id.caliber_object_id().id_number +" and Version ='" + treestuff[i].version_info.major_version_number + "." + treestuff[i].version_info.minor_version_number+"'";

								ResultSet testresult = stmt.executeQuery(SQLTestExist);

								while (testresult.next()) 
								{
									ItExists=true;
								}
								testresult.close();
							}
						}

						if(!ItExists)
						{
							ArrayList<Integer> reqList = new ArrayList<Integer>();
							if(CURRENTONLY.equalsIgnoreCase("currentonly"))
							{
								//delete req_info for req
								String SQLDelete="DELETE FROM REQ_INFO WHERE Req_ID =" + treestuff[i].object_id.caliber_object_id().id_number;
								stmt.execute(SQLDelete);
							}
							reqList.clear();
							reqList.add(treestuff[i].object_id.caliber_object_id().id_number);
							int reqid= treestuff[i].object_id.caliber_object_id().id_number;
							boolean resukts = AddReqInfo(reqid,session,projectID,baselineID,level,treestuff[i].version_info,reqman,myFrame);
							//addReqInfo(treestuff[i].object_id.caliber_object_id().id_number, treestuff[i].version_info,level,stmt, treestuff[i].depth, baselineID, LocaliseString(myType),LocaliseString(baseName));
						}

						

						if(i%100==0)
						{
							System.out.append(".");
						}
						

					}
					catch(Exception Ex){
						System.out.println("SQL Err: "+Ex.getMessage());
						System.out.println("error point:"+errorpoint );

					}
					anArray[i]= hiearchy;
					//*******************************************************************************************

				}
			}
			long finishtime = System.currentTimeMillis();
			//stmt.executeBatch();
			long overalltime = (finishtime - starttime)/1000;
			//System.out.println(LocaliseString(myproject) + "," + LocaliseString(baseName));
			System.out.println("FINISHED-->:"+LocaliseString(myproject) +"~"+ projectID +"~," + LocaliseString(baseName)+"~"+baselineID+"~ Tree Regeneration Complete! " + overalltime);
			
		}
		catch(Exception ex) {}
		Thread.currentThread().isAlive();
		Thread.currentThread().run();
		return true;
	}//end loadtree

	public static boolean AddReqInfo(int UpDateList,Session session,int projectID,int baselineID,String level,VersionInfo version,RequirementManager_v102 reqman,FrameworkManager myFrame)
	{
		
		errorpoint ="AddReqInfo " + UpDateList ;

		int reqid=UpDateList;
		if(reqid==1855)
		{
			System.out.println();
		}
		//build if exists
		int maj=version.major_version_number;
		int min=version.minor_version_number;
		String versionstring=maj + "." + min;
		
		try {

			

			errorpoint ="AddReqInfo + RequirementManager + frameworkmanager";

			int countupdate =0;

			long strt = System.currentTimeMillis();


			countupdate++;
			if(countupdate%100==0)
			{
				long fin = System.currentTimeMillis();
				long dur = (fin -strt);
				//System.out.println("Time taken: " + (fin-strt)/1000);
				System.out.append(".");
				strt = System.currentTimeMillis();
			}



			errorpoint ="AddReqInfo + RequirementManager + frameworkmanager + req " +reqid;

			if(reqid!=projectID || reqid!=baselineID)
			{

				long strttest = System.currentTimeMillis();

				long fintest = System.currentTimeMillis();
				try
				{
					RequirementInfo reqinfo =reqman.getRequirementVersionInfo(reqid, version);

					errorpoint =errorpoint + " " +  reqid + " " + version;
					String reqtype="";
					
					try{
						//get version from tree if not current
						errorpoint="baseline name query";
						//use Caliber to get baseline info

						String SQLBase= "SELECT Name from Baseline_INFO WITH(UPDLOCK , READPAST) WHERE BaselineID =" + baselineID;
						ResultSet resultBase = stmt.executeQuery(SQLBase);
						String basemane="";
						while (resultBase.next()) 
						{
							basemane=resultBase.getString("Name");
						}
						resultBase.close();
						RequirementInfo reqinfoV =null;
						VersionInfo Vers = new VersionInfo();
						Vers=version;
						String treeversion=Vers.major_version_number + "." + Vers.minor_version_number;
						if(!basemane.equalsIgnoreCase("Current Baseline"))
						{

							//18/12/2013
							String SQLinsert3 = "DELETE FROM Trace_INFO WHERE Req_ID_FROM =" + reqid + " AND Version_FROM ='" + Vers.major_version_number + "." + Vers.minor_version_number+ "'";
							try{
								stmt.execute(SQLinsert3);
							}
							catch(Exception ex){}
							String[] splitres = treeversion.split("[.]");

							reqinfoV = reqman.getRequirementVersionInfo(reqid,Vers);

						}
						else{
							RequirementInfo[] reqlat = reqman.getRequirementInfoLatestVersion(reqid);
							reqinfoV = reqlat[0];

							//12/03/2015 -sql issue
							String SQLinsert3 = "DELETE FROM Trace_INFO WHERE Req_ID_FROM =" + reqid + " AND Version_FROM ='" + treeversion+"'";
							try
							{
								stmt.execute(SQLinsert3);
							}
							catch(Exception ex){}
						}

						int Depth =-1;
						//responsibility
						try
						{
							//System.out.println("--------->Responsibility:" + reqid);
							//loop through groups and members assigned to requirements
							Any rv = ORB.init().create_any();
							//System.out.println("------------------->"+reqinfoV.type);
							rv.insert_string("RequirementType_p::id_number = "+ reqinfoV.type);
							RequirementTypeInfo[] tyeinfo = reqman.getRequirementTypeInfo(false, rv);
							reqtype=tyeinfo[0].name;

							Any rvV = ORB.init().create_any();

							//need to obtain creation and mod date for requirement version
							//Query Requirement object 
							rvV.insert_string("id_number =" + reqid);

							//RequirementRevisionInfo[] reqRev=session.getRemoteRequirementManager().getRequirementRevisionInfo(rvV);

							//update mod date
							String moddate="";
							int no_of_revisions=0;
							//updated orginal versions to version 
							int majV=version.major_version_number;
							int minV=version.minor_version_number;
							String created=ReturnCreationDate(reqid);
							String temp=ReturnModDate(reqid,majV,minV);

							moddate=temp.split("[|]")[0];

							no_of_revisions=Integer.parseInt(temp.split("[|]")[1]);

							strttest = System.currentTimeMillis();

							fintest = System.currentTimeMillis();

							//loop through groups and members assigned to requirements
							strttest = System.currentTimeMillis();
							for(int g=0;g<reqinfoV.group_member_info.length ;g++)
							{
								for(int m=0;m<reqinfoV.group_member_info[g].members.length ;m++)
								{
									try
									{
										int member = reqinfoV.group_member_info[g].members[m];
										//query for member userid and update SQL
										UserInfo[] userinfo =null;
										String userid="";
										Any rv3 = ORB.init().create_any();


										try
										{
											rv3.insert_string("User_v300_p::id_number = " + member);
											userinfo = myFrame.getUserInfo(rv3);
											userid=userinfo[0].user_id;
										}
										catch(Exception ex)
										{
											userid="User " + member + " Deleted";
										}
										String almlink="<A href=''"+"";

										if(baselinename.equalsIgnoreCase("Current Baseline"))
										{

											almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

										}
										else
										{
											almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

										}

										int no_chgs = reqinfo.available_versions.length;

										String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + "3" +",'Responsible',30,'" + LocaliseString(userid) +"','"+almlink+ "',"+no_chgs+",'"+updatetime+"')";

										//System.out.println (SQLinsert);
										stmt.addBatch(SQLinsert);
										
									}
									catch(Exception ex)
									{
										System.out.println(ex.getMessage());	
									}

								}

							}
						}
						catch(Exception ex){}
						fintest = System.currentTimeMillis();


						strttest = System.currentTimeMillis();
						Traces Traces = reqinfoV.trace_info;
						for(int t=0;t<Traces.traces_from.length ;t++)
						{
							TraceInfo traceF = Traces.traces_from[t];
							//depending on type query  generic objects

							int suspect=0;
							if(traceF.suspect==true)
							{
								suspect=1;
							}
							else
							{
								suspect=0;
							}
							String userdata="";
							if(traceF.from_object.object_type.value()!=4 && STYES.equalsIgnoreCase("STYES"))
							{
								//this is an external trace
								// Call StarTeam Function
								Any rv3 = ORB.init().create_any();
								rv3.insert_string("CaliberBase_p::id_number = " + traceF.from_object.id_number);

								GenericObjectInfo[] ext_Trace = myFrame.getGenericObjectInfo(rv3);
								userdata = ext_Trace[0].user_data;

								try
								{
									if(userdata.contains("StarTeam")&& STYES.equalsIgnoreCase("STYES"))
									{
										if(!userdata.contains("Backlog"))
										{


											String STresult=StarTeamInfo(userdata);
											//String SQLinsert = "INSERT INTO StarTeam_Traces VALUES (" + myprojectID + "," +baseid_number + "," +reqinfoV.id_number + ",'" + + version.major_version_number+ "." + version.minor_version_number + STresult +"," + traceT.suspect + ",'To'" +")";
											//check if exists, if so delete
											try
											{
												String SQLDeleteTrace="Delete From StarTeam_Traces where traceID=" + traceF.from_object.id_number + " and BaselineID=" + baselineID + " and Req_ID=" + reqinfoV.id_number;
												stmt.execute(SQLDeleteTrace);
											}
											catch(Exception ex){}

											//String SQLinsert = "INSERT INTO StarTeam_Traces VALUES (" + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"','" + STresult +"','" + traceF.suspect + "','From'," +traceF.from_object.id_number+")";
											String SQLinsert = "INSERT INTO StarTeam_Traces VALUES (" + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"','" + STresult +"','"+ traceF.suspect+ "','From'," +traceF.from_object.id_number+")";
											//System.out.println("StarTeam:" +SQLinsert);
											stmt.execute(SQLinsert);
											//con.commit();
										}
									}
									if(userdata.contains("Quality Center") && QCYES.equalsIgnoreCase("QCYES"))
									{
										try
										{
											String SQLDeleteTrace="Delete From QC_Traces where traceID=" + traceF.from_object.id_number + " and BaselineID=" + baselineID + " and Req_ID=" + reqinfoV.id_number;
											stmt.execute(SQLDeleteTrace);
										}
										catch(Exception ex){}
										//get QC information from trace
										//insert it into QC table for requirement version
										String QCresult=QCInfo(userdata);
										try{
											String SQLinsert = "INSERT INTO QC_Traces VALUES (" + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"','" + QCresult +"','"+ traceF.suspect+ "','From'," +traceF.from_object.id_number+")";
											//System.out.println("QC_Traces:" +SQLinsert);
											stmt.execute(SQLinsert);
										}
										catch(Exception e)
										{
											System.out.println(e.getMessage());
										}
									}
									/*if(userdata.contains("Together"))
										{
											System.out.println("Together Trace");
											String SQLtog = "INSERT INTO StarTeam_Traces VALUES (" + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"','" + userdata +"',,,,,,,,'"+ traceF.suspect+ "','From'," +traceF.from_object.id_number+")";
											stmt.execute(SQLtog);
										}*/

								}
								catch(Exception e)
								{
									System.out.println("External From Trace:"+e.getMessage());
								}
							}
							//System.out.println(reqinfoV.id_number + " " + treeversion + " From " + traceF.from_object.id_number);
							else{
								try
								{
									//18/12/2013
									//String SQLinsert = "INSERT INTO Trace_From VALUES (" + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"','From' ,"+traceF.from_object.id_number +","+suspect+","+ traceF.from_object.object_type.value() +",'"+ userdata +"')";
									//stmt.executeUpdate(SQLinsert);
									//con.commit();
									
									
									String SQLinsert = GetTraceInfo(traceF.id_number);
									//String SQLinsert = "INSERT INTO Trace_To VALUES ("  + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"','To' ,"+traceT.to_object.id_number + "," +suspect+","+ traceT.to_object.object_type.value()+",'"+ userdata +"')";

									stmt.execute(SQLinsert);
									
								}
								catch(Exception e)
								{
									System.out.println("Requirement Trace From:"+e.getMessage());
								}
							}
						}

						int no_tracesto =0;
						int no_tracessuspect =0;

						for(int t=0;t<Traces.traces_to.length ;t++)
						{

							TraceInfo traceT = Traces.traces_to[t];
							int suspect=0;
							if(traceT.suspect==true)
							{
								suspect=1;
								no_tracessuspect++;
							}
							else
							{
								suspect=0;
							}
							String userdata="";
							if(traceT.to_object.object_type.value()!=4 )
							{
								//this is an external trace
								// Call StarTeam Function
								Any rv3 = ORB.init().create_any();
								rv3.insert_string("CaliberBase_p::id_number = " + traceT.to_object.id_number);

								GenericObjectInfo[] ext_Trace = myFrame.getGenericObjectInfo(rv3);
								userdata = ext_Trace[0].user_data;


								try
								{
									if(STYES.equalsIgnoreCase("STYES"))
									{
										if(userdata.contains("StarTeam")&& STYES.equalsIgnoreCase("STYES"))
										{
											if(!userdata.contains("Backlog"))
											{
												String STresult=StarTeamInfo(userdata);
												//String SQLinsert = "INSERT INTO StarTeam_Traces VALUES (" + myprojectID + "," +baseid_number + "," +reqinfoV.id_number + ",'" + + version.major_version_number+ "." + version.minor_version_number + STresult +"," + traceT.suspect + ",'To'" +")";
												try
												{
													String SQLDeleteTrace="Delete From StarTeam_Traces where traceID=" + traceT.to_object.id_number + " and BaselineID=" + baselineID + " and Req_ID=" + reqinfoV.id_number;
													stmt.execute(SQLDeleteTrace);
												}
												catch(Exception ex){}
												String SQLinsert = "INSERT INTO StarTeam_Traces VALUES (" + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"','" + STresult +"','" + traceT.suspect + "','To'," +traceT.to_object.id_number+")";

												//System.out.println("StarTeam:" +SQLinsert);
												stmt.execute(SQLinsert);
												//con.commit();
											}
										}
									}
									if(userdata.contains("Quality Center") && QCYES.equalsIgnoreCase("QCYES"))
									{
										//get QC information from trace
										//insert it into QC table for requirement version
										try
										{
											String SQLDeleteTrace="Delete From QC_Traces where traceID=" + traceT.to_object.id_number + " and BaselineID=" + baselineID + " and Req_ID=" + reqinfoV.id_number;
											stmt.execute(SQLDeleteTrace);
										}
										catch(Exception ex){}
										String QCresult=QCInfo(userdata);
										try
										{
											String SQLDeleteTrace="Delete From QC_Traces where traceID=" + traceT.to_object.id_number + " and BaselineID=" + baselineID + " and Req_ID=" + reqinfoV.id_number;
											stmt.execute(SQLDeleteTrace);
										}
										catch(Exception ex){}
										try{
											String SQLinsert = "INSERT INTO QC_Traces VALUES (" + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"'," + QCresult +",'"+ traceT.suspect+ "','To'," +traceT.from_object.id_number+",'" +userdata+"'," + traceT.to_object.id_number+")";
											//System.out.println("QC_Traces:" +SQLinsert);
											stmt.execute(SQLinsert);
										}
										catch(Exception e)
										{
											System.out.println(e.getMessage());
										}
									}
								}
								catch(Exception e)
								{
									System.out.println(e.getMessage());
								}
							}
							else{
								try{
									no_tracesto++;

									//18/12/2013

									String SQLinsert = GetTraceInfo(traceT.id_number);
									//String SQLinsert = "INSERT INTO Trace_To VALUES ("  + projectID + "," +baselineID + "," +reqinfoV.id_number + ",'" + treeversion +"','To' ,"+traceT.to_object.id_number + "," +suspect+","+ traceT.to_object.object_type.value()+",'"+ userdata +"')";

									stmt.execute(SQLinsert);
									//con.commit();
								}

								catch(Exception ex)
								{}
							}


						}

						errorpoint="Tree version";

						fintest = System.currentTimeMillis();
						//System.out.println("traces took" + (fintest-strttest));

						int status=reqinfoV.status;
						int priority=reqinfoV.priority;
						int owner = reqinfoV.owner;
						strttest = System.currentTimeMillis();
						//Requirement Name
						try{
							//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number + ";" + "1" +";Requirement Name;1;" + reqinfoV.name.replaceAll("(\\r|\\n)+", " ") + ";" );
							String almlink="<A href=''"+"";
							//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
							if(baselinename.equalsIgnoreCase("Current Baseline"))
							{

								almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

							}
							else
							{
								almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

							}
							String reqname = reqinfoV.name.replaceAll("(\\r|\\n)+", " ");

							String SQLinsert = "INSERT INTO REQ_INFO VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + "1" +",'Requirement Name',1,'" + LocaliseString(reqname) +"','"+almlink+ "','','"+updatetime+"')";
							//System.out.println (SQLinsert);
							stmt.addBatch(SQLinsert);
							//con.commit();
							//UPDATE Tree with new name
							//SQLUPDATETREE ="UPDATE Tree Set Req_Name='" + LocaliseString(reqname)+"',Version='"+treeversion + "' WHERE Req_ID=" + reqid  + " AND Baseline_ID =" + baselineID;
							//stmt.execute(SQLUPDATETREE);


							//con.commit();
						}
						catch(Exception e)
						{
							pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";Requirement Name;" + e.getLocalizedMessage());

						}

						//Requirement Description
						try{
							int desctype =  reqinfoV.description.discriminator().value();
							// //pioUDAs.println(reqinfoV.description.discriminator().value());

							if(desctype==0)
							{
								//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number + ";" + "2" +";Requirement Description;2;" + "\"" +reqinfoV.description.html_text().replaceAll("(\\r|\\n)+", " ")+"\"" + ";" );


								String thedesc=reqinfoV.description.html_text().replaceAll("(\\r|\\n)+", " ");

								//thedesc=Translate.encode(thedesc);

								try{
									//hello
									//if(thedesc.contains("<img") || thedesc.contains("<IMG"))
									//{

									ImageManager im = session.getImageManager();

									im.populateCache(thedesc);
									thedesc=LocaliseString(thedesc);
									//encode images 30/04/15
									//thedesc=encodeHTMLmages(thedesc);
									//}
								}
								catch(Exception e)
								{
									//System.out.println (e.getMessage());
								}

								//thedesc=thedesc.replaceAll("<p> </p>", "");
								//thedesc=escapeHTML(thedesc);
								//thedesc=thedesc.replaceAll("<p>&#160;</p>", "");
								//thedesc=thedesc.replaceAll("&nbsp;", " ");
								//thedesc=thedesc.replaceAll("&#194;&#160;", "&#160;");
								//thedesc=thedesc.replaceAll("&#194;&#183;", "&#183;");
								//thedesc=thedesc.replaceAll("<head></head>", "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"></head>");

								String almlink="<A href=''"+"";
								//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
								if(baselinename.equalsIgnoreCase("Current Baseline"))
								{

									almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

								}
								else
								{
									almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

								}
								
								String SQLinsert = "INSERT INTO REQ_INFO VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + "2" +",'Requirement Description',2,'" + thedesc   +"','"+almlink+ "','','"+updatetime+"')";
								
								
								//System.out.println (SQLinsert);
								stmt.addBatch(SQLinsert);
								//con.commit();
							}
							if(desctype==2)
							{
								//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number + ";" + "3" +";Requirement Mapped Description;3;" +"\""+ reqinfoV.description.slave_description().description.replaceAll("(\\r|\\n)+", " ")+"\"" + ";" );
								String thedesc=reqinfoV.description.slave_description().description.replaceAll("(\\r|\\n)+", " ");

								


								//thedesc=thedesc.replaceAll("<p>&#160;</p>", "");
								//thedesc=Translate.encode(thedesc);
								//thedesc=thedesc.replaceAll("<p> </p>", "");
								//thedesc=escapeHTML(thedesc);
								//thedesc=thedesc.replaceAll("<p>&#160;</p>", "");
								//thedesc=thedesc.replaceAll("&nbsp;", " ");
								//thedesc=thedesc.replaceAll("&#194;&#160;", "&#160;");
								//thedesc=thedesc.replaceAll("&#194;&#183;", "&#183;");
								//thedesc=thedesc.replaceAll("<head></head>", "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"></head>");

								ImageManager im = session.getImageManager();
								try{
									im.populateCache(thedesc);
									thedesc=LocaliseString(thedesc);
									//encode images 30/04/15
									//thedesc=encodeHTMLmages(thedesc);
								}
								catch(Exception e){}
								String almlink="<A href=''"+"";
								//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
								if(baselinename.equalsIgnoreCase("Current Baseline"))
								{

									almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

								}
								else
								{
									almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

								}
								String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + "3" +",'Requirement Description',3,'" + thedesc  +"','"+almlink+ "','','"+updatetime+"')";
								////System.out.println (SQLinsert);
								stmt.addBatch(SQLinsert);
								//con.commit();
								//changed 03/09/2012
								ArrayList<Integer> masterdescids= new ArrayList<Integer>();
								//System.out.println(reqinfoV.description.slave_description().master_requirement_id);
								masterdescids.add(reqinfoV.description.slave_description().master_requirement_id);

								try{
									//System.out.println("Processing Master:"+reqinfoV.description.slave_description().master_requirement_id);
									RequirementInfo[] masterreqthing = reqman.getRequirementInfoLatestVersion(reqinfoV.description.slave_description().master_requirement_id);
									int slavereqid= reqinfoV.description.slave_description().master_requirement_id;
									AddReqInfo(slavereqid,session,projectID,baselineID,level,masterreqthing[0].available_versions[0],reqman,myFrame);
								}
								catch(Exception ex)
								{
									System.out.println("---->EEEEEERRRRRR"+ex.getMessage());
								}




							}
							if(desctype==1)
							{
								//System.out.println ("---------------->Master");	
								String thedesc=reqinfoV.description.master_description().description.replaceAll("(\\r|\\n)+", " ");

								

								//String thedesc=reqinfoV.description.master_description().description.replaceAll("(\\r|\\n)+", " ");
								//thedesc=thedesc.replaceAll("'", "");

								//changed 03/09/2012
								//ArrayList masterdescids= new ArrayList();
								//System.out.println(reqinfoV.description.slave_description().master_requirement_id);
								//masterdescids.add(reqinfoV.description.slave_description().master_requirement_id);



								//								try{
								//								AddReqInfo(masterdescids,session,projectID,baselineID,level);
								//								}
								//								catch(Exception ex)
								//								{
								//									System.out.println("---->EEEEEERRRRRR"+ex.getMessage());
								//								}



								//thedesc=thedesc.replaceAll("<p>&#160;</p>", "");
								//thedesc=Translate.encode(thedesc);
								//thedesc=thedesc.replaceAll("<p> </p>", "");
								//thedesc=escapeHTML(thedesc);
								//thedesc=thedesc.replaceAll("<p>&#160;</p>", "");
								//thedesc=thedesc.replaceAll("&nbsp;", " ");
								//thedesc=thedesc.replaceAll("&#194;&#160;", "&#160;");
								//thedesc=thedesc.replaceAll("&#194;&#183;", "&#183;");
								//thedesc=thedesc.replaceAll("<head></head>", "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"></head>");

								ImageManager im = session.getImageManager();
								try{
									im.populateCache(thedesc);
									//encode images 30/04/15
									thedesc=LocaliseString(thedesc);
									//thedesc=encodeHTMLmages(thedesc);
								}
								catch(Exception e){}
								String almlink="<A href=''"+"";
								//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
								if(baselinename.equalsIgnoreCase("Current Baseline"))
								{

									almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

								}
								else
								{
									almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

								}
								String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + "3" +",'Requirement Description',3,'" + thedesc  +"','"+almlink+ "','','"+updatetime+"')";
								////System.out.println (SQLinsert);
								stmt.addBatch(SQLinsert);
								//con.commit();
								int[] slaveid = reqinfoV.description.master_description().slave_requirement_ids;
								for(int s=0;s<slaveid.length ;s++)
								{
									//almlink=almlink.replaceAll("Req:"+ reqid,"Req:" +slaveid[s]);
									//RequirementManager_v102 reqman = session.getRemoteRequirementManager();
									RequirementInfo[] reqmapinfo = reqman.getRequirementInfoLatestVersion(slaveid[s]);
									String mapname = reqmapinfo[0].name;
									int projid = reqmapinfo[0].project;
									Any rv2 = ORB.init().create_any();
									rv2.insert_string("Project_p::id_number= " + projid );
									ProjectInfo[] projinfo = myFrame.getProjectInfo(rv2);
									rv2.insert_string("RequirementType_p::id_number= " + reqmapinfo[0].type);
									RequirementTypeInfo_v102[] typeinfo = reqman.getRequirementTypeInfo_v102(true, rv2);
									String typename=typeinfo[0].name;
									String mapdesc= "Project: " + projinfo[0].name + "Type: " + typename + " Req: " + mapname + "("+ slaveid[s] +")";
									mapdesc=LocaliseString(mapdesc);


									String almlink2=almlink.replaceAll("Req:"+ reqid,mapdesc);
									//obtain project of shared

									reqmapinfo = reqman.getRequirementInfoLatestVersion(slaveid[s]);
									int mapProj = reqmapinfo[0].project;
									almlink2=almlink2.replaceAll("/"+ reqid,"/" +slaveid[s]);
									almlink2=almlink2.replaceAll("20000_"+reqinfoV.project,"20000_"+ mapProj);

									SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + "3" +",'Mapping',3,'" + almlink2 + "','"+almlink+"','','"+updatetime+"')";
									////System.out.println (SQLinsert);
									stmt.addBatch(SQLinsert);
									//con.commit();
									String SQLupdatemapfag= "UPDATE Tree  Set map_flag='" + "<img src=\"\\ORB\\Styles\\images\\mapped.jpg \" alt=\"Mapped Requirement\" title=\"Mapped\" />'" + " WHERE Req_ID= " + reqid + " AND Version = '" +treeversion+ "'" + " AND Baseline_ID= " + baselineID;
									stmt.addBatch(SQLupdatemapfag);
									//con.commit();
								}


							}
						}
						catch(Exception e)
						{
							pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";Requirement Description;" + e.getLocalizedMessage());

						}


						//Requirement Validation
						try{
							//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number + ";" + "4" +";Validation;4;" + reqinfoV.test_description.replaceAll("(\\r|\\n)+", " ") + ";" );
							String theVal=reqinfoV.test_description.replaceAll("(\\r|\\n)+", " ");
							//theVal=theVal.replaceAll("'", "");
							String almlink="<A href=''"+"";
							//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
							if(baselinename.equalsIgnoreCase("Current Baseline"))
							{

								almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

							}
							else
							{
								almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

							}
							String SQLinsert = "INSERT INTO REQ_INFO VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + "4" +",'Validation',4,'" + LocaliseString(theVal)  +"','"+almlink+ "','','"+updatetime+"')";
							////System.out.println (SQLinsert);
							stmt.addBatch(SQLinsert);
							//con.commit();
						}
						catch(Exception e)
						{
							pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";Validation;" + e.getLocalizedMessage());

						}

						
						//Requirement Owner
						try{
							//System.out.println("--------->Requirement Owner:" + reqid);
							//query DB for user	
							Any rv6 = ORB.init().create_any();
							// //pioUDAs.println(theSSL2);
							UserInfo[] userinfo4 =null;
							String userid="";
							try
							{
								rv6.insert_string("User_v300_p::id_number = "+ owner);

								userinfo4 = myFrame.getUserInfo(rv6);
								userid=userinfo4[0].user_id;
							}
							catch(Exception ex)
							{
								userid="User " + owner + " Deleted";
							}

							//pioUDAs.println(projid_number+";"+ baseid_number+",'"+ reqtype+"'"+";"+treestuff[i].depth+";"+treestuff[i].object_id.caliber_object_id().id_number + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number + ";" + "5" +";Requirement Owner;5;" + userinfo4[0].user_id + ";" );
							String almlink="<A href=''"+"";
							if(baselinename.equalsIgnoreCase("Current Baseline"))
							{

								almlink="<A href=''"+"alm://caliberrm!"+ server.getHost()+"_20000_" + projectID+"/"+ reqid+ ";ns=requirement'"+"'>Req:"+reqid+"</A>";

							}
							else
							{
								almlink="<A href=''"+"alm://caliberrm!"+ server.getHost()+"_20000_" + projectID+"/"+ reqid+ ";ns=requirement?baseline=" +baselineID +"''>Req:"+reqid+"</A>";

							}

							String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"',"+Depth+","+ reqid + ",'" + treeversion + "'," + "5" +",'Requirement Owner',"+owner+",'" +LocaliseString(userid)+ "','"+almlink+"','','"+updatetime+"')";
							//System.out.println (SQLinsert);
							stmt.addBatch(SQLinsert);
							//con.commit();

						}
						catch(Exception e)
						{
							pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";Requirement Owner;" + e.getLocalizedMessage());
							String almlink="<A href=''"+"";
							if(baselinename.equalsIgnoreCase("Current Baseline"))
							{

								almlink="<A href=''"+"alm://caliberrm!"+ server.getHost()+"_20000_" + projectID+"/"+ reqid+ ";ns=requirement'"+"'>Req:"+reqid+"</A>";

							}
							else
							{
								almlink="<A href=''"+"alm://caliberrm!"+ server.getHost()+"_20000_" + projectID+"/"+ reqid+ ";ns=requirement?baseline=" +baselineID +"''>Req:"+reqid+"</A>";

							}
							String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + "5" +",'Requirement Owner',"+owner+",'" + "Missing" + "','"+almlink+"','','"+updatetime+"')";
							////System.out.println (SQLinsert);
							stmt.addBatch(SQLinsert);
							//con.commit();
						}

						try{
							//Status information, parse bytes
							Any rv4 = ORB.init().create_any();
							rv4.insert_string("UDA_p::name like \"Requirement?Status\"");
							UDAInfo[] UDA_info2 = session.getRemoteRequirementManager().getUDAInfo(rv4);


							//InputStream UDAstuff2 = UDA_info2[0].uda_info.type_info.create_input_stream();
							// //pioUDAs.println(UDAstuff2);
							ArrayList<String> strArray2 = new ArrayList<String>();
							UDASingleSelectionListInfo slist = Caliber.UDASingleSelectionListInfoHelper.extract(UDA_info2[0].uda_info.type_info);
							UDASelectionListEntry[] entrys = slist.selection_list;
							for(int e=0;e<entrys.length;e++)
							{
								strArray2.add(entrys[e].value);

							}

							// //pioUDAs.println("Status= "+strArray2.get(status));
							String statusValue="";//strArray2.get(status).toString().replaceAll("'", "''");
							try{
								statusValue=strArray2.get(status).toString();//.replaceAll("'", "''");

							}
							catch (Exception ex)
							{
								statusValue="";
							}
							//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number + ";" +UDA_info2[0].uda_info.type.value() +";Requirement Status;6;" + strArray2.get(status)+ ";" );
							String almlink="<A href=''"+"";
							//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
							if(baselinename.equalsIgnoreCase("Current Baseline"))
							{

								almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

							}
							else
							{
								almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

							}
							String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "',12,'Requirement Status',6,'" +LocaliseString(statusValue)+"','"+almlink+ "','','"+updatetime+"')";
							////System.out.println (SQLinsert);
							stmt.addBatch(SQLinsert);
							//con.commit();
						}
						catch(Exception e)
						{
							pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";Requirement Status;" + e.getLocalizedMessage());

						}

						try{
							//Get Priority Stuff
							Any rv5 = ORB.init().create_any();
							rv5.insert_string("UDA_p::name like \"Requirement?Priority\"");
							UDAInfo[] UDA_info3 = session.getRemoteRequirementManager().getUDAInfo(rv5);
							// //pioUDAs.println(UDA_info3[0].uda_info.name);
							// //pioUDAs.println(UDA_info3[0].uda_info.type.value());

							//InputStream UDAstuff3 = UDA_info3[0].uda_info.type_info.create_input_stream();
							//pioUDAs.println(UDAstuff3);
							ArrayList<String> strArray3 = new ArrayList<String>();
							UDASingleSelectionListInfo slist = Caliber.UDASingleSelectionListInfoHelper.extract(UDA_info3[0].uda_info.type_info);
							UDASelectionListEntry[] entrys = slist.selection_list;
							for(int e=0;e<entrys.length;e++)
							{
								strArray3.add(entrys[e].value);

							}

							// //pioUDAs.println("Pirority= "+strArray3);
							String PriorityValue =""; //strArray3.get(priority).toString().replaceAll("'", "''");
							try{
								PriorityValue =strArray3.get(priority).toString();//.replaceAll("'", "''");

							}
							catch (Exception ex)
							{
								PriorityValue="";
							}
							//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number + ";" +UDA_info3[0].uda_info.type.value() +";Requirement Priority;7;" + strArray3.get(priority)+ ";" );
							String almlink="<A href=''"+"";
							//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
							if(baselinename.equalsIgnoreCase("Current Baseline"))
							{

								almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

							}
							else
							{
								almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

							}
							String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "',12,'Requirement Priority',7,'" +LocaliseString(PriorityValue) +"','"+almlink+ "','','"+updatetime+"')";
							////System.out.println (SQLinsert);
							stmt.addBatch(SQLinsert);
							//con.commit();
						}
						catch(Exception e)
						{
							pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";Requirement Priority;" + e.getLocalizedMessage());

						}

						fintest = System.currentTimeMillis();
						//System.out.println("stanard UDAs took" + (fintest-strttest));
						//System.out.println("number of udas to process: " + reqinfoV.uda_values.length);
						for (int u=0; u<reqinfoV.uda_values.length;u++)
						{
							//int reqid =   reqinfoV.uda_values[u].uda_id_number;
							strttest = System.currentTimeMillis();

							int UDA_type = reqinfoV.uda_values[u].value.type().kind().value();
							UDAInfo[] UDA_info = null;

							// //pioUDAs.println("-------> " + UDA_type );
							//obtain name of UDA
							Any rv1 = ORB.init().create_any();
							rv1.insert_string("UDA_p::id_number = " + reqinfoV.uda_values[u].uda_id_number);
							UDA_info = session.getRemoteRequirementManager().getUDAInfo(rv1);
							//System.out.println(UDA_info[0].uda_info.name + " "+ UDA_type);
							eUDAType thetype = UDA_info[0].uda_info.type;
							//System.out.println(UDA_info[0].uda_info.name + " "+ UDA_type + " " + thetype.value());
							// //pioUDAs.println("---->"+thetype.value());

							//Multiple Selection lists
							//System.out.println("UDA Type:" + UDA_type + " " +  reqinfoV.id_number);
							try{
								if (UDA_type==21)
								{
									//System.out.println(thetype.value());

									//obtain index of values selected
									if(thetype.value()==13 && reqinfoV.uda_values[u].value.toString().contains("CORBA_ShortSeq")) //for multiselectionlist
									{
										short[] MSLvalue = ShortSeqHelper.read(reqinfoV.uda_values[u].value.create_input_stream());

										if(MSLvalue.length==0)
										{
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}
											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'','"+almlink+ "','','"+updatetime+"')";
											////System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();
										}

										//for (int msl=0;msl<MSLvalue.length;msl++)
										//{
										// //pioUDAs.println (UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + MSLvalue[msl]);
										// //pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + MSLvalue[msl]);
										//pio.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + MSLvalue[msl]);
										//find value from uda
										//InputStream UDAstuff = UDA_info[0].uda_info.type_info.create_input_stream();


										//System.out.println(UDAstuff);

										ArrayList<String> strArray = new ArrayList<String>();
										UDAMultipleSelectionListInfo list = Caliber.UDAMultipleSelectionListInfoHelper.extract(UDA_info[0].uda_info.type_info);
										if(list !=null)
										{	
											UDASelectionListEntry[] entrys = list.selection_list;
											for(int e=0;e<entrys.length;e++)
											{
												strArray.add(entrys[e].value);

											}
										}
										else
										{
											strArray.add("");
										}

										//System.out.println(strArray);
										//short[] MSLvalue = shorthelp.read(reqinfo[0].uda_values[u].value.create_input_stream());


										for (int msl=0;msl<MSLvalue.length;msl++)
										{

											//String MSLValue=strArray.get(MSLvalue[msl]).toString();//.replaceAll("'", "''");
											//SSLValue=SSLValue+","+ MSLValue;


											//}

											// //pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + MSLvalue[msl]);

											//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + strArray.get(MSLvalue[msl])+";");
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}
											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(strArray.get(MSLvalue[msl]).toString()) +"','"+almlink+ "','','"+updatetime+"')";
											////System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();
										}

									}
									else if(thetype.value()==7)
									{
										int[] MSGL = LongSeqHelper.read(reqinfoV.uda_values[u].value.create_input_stream());

										if(MSGL.length==0)
										{
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}
											//String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + "," + treeversion + "," + UDA_type +",'',"+reqinfoV.uda_values[u].uda_id_number +"','"+almlink+ "','')";
											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'','"+almlink+ "','','"+updatetime+"')";
											////System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();
										}

										for(int g=0;g<MSGL.length;g++)
										{
											// //pioUDAs.println(MSGL[g]);
											//query db for group
											Any rv2 = ORB.init().create_any();
											rv2.insert_string("Group_v300_p::id_number = "+ MSGL[g]);
											GroupInfo[] grpinfo = myFrame.getGroupInfo(rv2);
											//grpinfo[0].name
											//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + grpinfo[0].name +";");
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}
											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(grpinfo[0].name) +"','"+almlink+ "','','"+updatetime+"')";
											////System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();
										}


									}
									else if(thetype.value()==5)
									{
										int[] MSUL = LongSeqHelper.read(reqinfoV.uda_values[u].value.create_input_stream());
										if(MSUL.length==0)
										{
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}
											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number +",'','"+almlink+ "','','"+updatetime+"')";
											//System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();
										}


										for(int g=0;g<MSUL.length;g++)
										{
											//System.out.println("--------->MSUL:" + reqid);
											// //pioUDAs.println(MSUL[g]);
											//query DB for user	
											UserInfo[] userinfo =null;
											String theuser="";
											try
											{
												Any rv2 = ORB.init().create_any();
												rv2.insert_string("User_v300_p::id_number = "+ MSUL[g]);

												userinfo = myFrame.getUserInfo(rv2);
												theuser= userinfo[0].user_id;
											}
											catch(Exception e)
											{
												theuser="Deleted User" + MSUL[g];
											}


											//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + userinfo[0].user_id+ ";");
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}
											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(theuser) +"','"+almlink+ "','','"+updatetime+"')";
											////System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();
										}

									}
									else
									{
										//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";");// + MSLvalue[msl]);
										String almlink="<A href=''"+"";
										//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
										if(baselinename.equalsIgnoreCase("Current Baseline"))
										{

											almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

										}
										else
										{
											almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

										}
										String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number +"','"+almlink+ "','','"+updatetime+"')";
										////System.out.println (SQLinsert);
										stmt.addBatch(SQLinsert);
										//con.commit();
									}


								}//if 21
							}
							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}

							try{
								if (UDA_type==8)//UDA Boolean
								{
									boolean bool =reqinfoV.uda_values[u].value.extract_boolean();
									//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + bool +";");
									String almlink="<A href=''"+"";
									//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
									if(baselinename.equalsIgnoreCase("Current Baseline"))
									{

										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

									}
									else
									{
										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

									}
									String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + bool +"','"+almlink+ "','','"+updatetime+"')";
									////System.out.println (SQLinsert);
									stmt.addBatch(SQLinsert);
									//con.commit();
								}
							}
							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}

							try{
								if (UDA_type==15) //UDA Date
								{
									String theDate = reqinfoV.uda_values[u].value.toString();
									theDate=theDate.replace("}", "");
									theDate=theDate.replace(";", "");
									// //pioUDAs.println(theDate);

									String[] realdates = theDate.split("=");
									// //pioUDAs.println(realdates[2]);
									long epoch =  Integer.parseInt(realdates[2]);

									//Date myDate= new Date(epoch);
									String realdate = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date (epoch*1000));
									//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + realdate+";");
									String almlink="<A href=''"+"";
									//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
									if(baselinename.equalsIgnoreCase("Current Baseline"))
									{

										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

									}
									else
									{
										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

									}
									String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + realdate +"','"+almlink+ "','','"+updatetime+"')";
									////System.out.println (SQLinsert);
									stmt.addBatch(SQLinsert);
									//con.commit();
								}
							}
							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}
							try{
								if (UDA_type==5) //UDA long
								{
									long Dur =reqinfoV.uda_values[u].value.extract_ulong();
									//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + Dur+";");
									String almlink="<A href=''"+"";
									//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
									if(baselinename.equalsIgnoreCase("Current Baseline"))
									{

										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

									}
									else
									{
										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

									}
									String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + Dur +"','"+almlink+ "','','"+updatetime+"')";
									////System.out.println (SQLinsert);
									stmt.addBatch(SQLinsert);
									//con.commit();
								}
							}
							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}

							try{
								if (UDA_type==6) //UDA float
								{
									float thefloat = reqinfoV.uda_values[u].value.extract_float();
									//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + thefloat+";");
									String almlink="<A href=''"+"";
									//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
									if(baselinename.equalsIgnoreCase("Current Baseline"))
									{

										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

									}
									else
									{
										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

									}
									String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + thefloat +"','"+almlink+ "','','"+updatetime+"')";
									////System.out.println (SQLinsert);
									stmt.addBatch(SQLinsert);
									//con.commit();
								}
							}
							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}

							try{
								if (UDA_type==3) //UDA long Integer
								{
									////System.out.println(thetype.value());
									if(thetype.value()==10)
									{
										int theLint = reqinfoV.uda_values[u].value.extract_long();

										//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + theLint+";");
										String almlink="<A href=''"+"";
										//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
										if(baselinename.equalsIgnoreCase("Current Baseline"))
										{

											almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

										}
										else
										{
											almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

										}
										String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + theLint +"','"+almlink+ "','','"+updatetime+"')";
										////System.out.println (SQLinsert);
										stmt.addBatch(SQLinsert);
										//con.commit();
									}

									if(thetype.value()==6) //for SSGL
									{
										int theLint = reqinfoV.uda_values[u].value.extract_long();
										//query db for group
										// //pioUDAs.println(theSSL2);
										Any rv2 = ORB.init().create_any();
										rv2.insert_string("Group_v300_p::id_number = " + theLint);
										GroupInfo[] grpinfo = myFrame.getGroupInfo(rv2);
										//grpinfo[0].name	
										if (grpinfo.length==0)
										{
											//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + ""+";");
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}
											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + ""  +"','"+almlink+ "','','"+updatetime+"')";
											////System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();
										}
										else
										{
											//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + grpinfo[0].name+";");
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}
											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(grpinfo[0].name)  +"','"+almlink+ "','','"+updatetime+"')";
											////System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();

										}
									}
								}
							}
							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}

							try{
								if (UDA_type==18) //UDA Text Line
								{
									String theText = reqinfoV.uda_values[u].value.extract_string();
									//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + theText+";");
									String almlink="<A href=''"+"";
									//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
									if(baselinename.equalsIgnoreCase("Current Baseline"))
									{

										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

									}
									else
									{
										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

									}
									String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(theText) +"','"+almlink+ "','','"+updatetime+"')";
									////System.out.println (SQLinsert);
									stmt.addBatch(SQLinsert);
									//con.commit();
								}
							}
							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}

							try{
								if (UDA_type==3 && thetype.value()==4)
								{
									//System.out.println("--------->SSUL:" + reqid);
									////System.out.println("Hello "+thetype.value());
									long theSSL2=0;
									theSSL2 = reqinfoV.uda_values[u].value.extract_long();
									// //pioUDAs.println("Long: "+theSSL2);
									//query DB for user	
									Any rv2 = ORB.init().create_any();
									// //pioUDAs.println(theSSL2);
									UserInfo[] userinfo =null;
									String userid="";
									try
									{
										rv2.insert_string("User_v300_p::id_number = "+ theSSL2);
										userinfo = myFrame.getUserInfo(rv2);
										userid=userinfo[0].user_id;
									}
									catch(Exception ex)
									{
										userid= "User " + theSSL2+" deleted";
									}


									//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + userinfo[0].user_id + ";");
									String almlink="<A href=''"+"";
									//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
									if(baselinename.equalsIgnoreCase("Current Baseline"))
									{

										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

									}
									else
									{
										almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

									}


									String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(userid) +"','"+almlink+ "','','"+updatetime+"')";
									////System.out.println (SQLinsert);
									stmt.addBatch(SQLinsert);
									//con.commit();

								}
							}
							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}

							try{
								if (UDA_type==2) //UDA SSL
								{
									//System.out.println(UDA_type);
									//System.out.println(thetype.value());
									short theSSL = 0;
									long theSSL2=0;
									// //pioUDAs.println("UDA Type:"+UDA_type);
									// //pioUDAs.println("SSL:"+thetype.value());
									if(thetype.value()!=10 )
									{
										////System.out.println("HELLOOOOOOO");
										try{
											theSSL = reqinfoV.uda_values[u].value.extract_short();
											// //pioUDAs.println("Short: "+theSSL);
										}
										catch (Exception e)
										{
											theSSL2 = reqinfoV.uda_values[u].value.extract_long();
											// //pioUDAs.println("Long: "+theSSL);
										}


										// //pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + theSSL);
										//need to stream in data
										//InputStream UDAstuff = UDA_info[0].uda_info.type_info.create_input_stream();
										// //pioUDAs.println(UDAstuff);
										// //pioUDAs.println(thetype.value());

										ArrayList<String> strArray = new ArrayList<String>();
										UDASingleSelectionListInfo slist = Caliber.UDASingleSelectionListInfoHelper.extract(UDA_info[0].uda_info.type_info);
										if(slist !=null)
										{	
											UDASelectionListEntry[] entrys = slist.selection_list;
											for(int e=0;e<entrys.length;e++)
											{
												strArray.add(entrys[e].value);

											}
										}
										else
										{
											strArray.add("");
										}

										if(thetype.value()==12) //for SSL
										{
											if (theSSL==-1)
											{
												//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + "-1"+ ";");
												String almlink="<A href=''"+"";
												//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
												if(baselinename.equalsIgnoreCase("Current Baseline"))
												{

													almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

												}
												else
												{
													almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

												}
												String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + "-1" +"','"+almlink+ "','','"+updatetime+"')";
												////System.out.println (SQLinsert);
												stmt.addBatch(SQLinsert);
												//con.commit();
											}
											else {
												//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + strArray.get(theSSL)+";");
												String almlink="<A href=''"+"";
												//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
												if(baselinename.equalsIgnoreCase("Current Baseline"))
												{

													almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

												}
												else
												{
													almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

												}
												String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(strArray.get(theSSL).toString())  +"','"+almlink+ "','','"+updatetime+"')";
												////System.out.println (SQLinsert);
												stmt.addBatch(SQLinsert);
												//con.commit();
											}
										}
										if(thetype.value()==4) //for SSUL
										{
											//System.out.println("--------->:" + reqid);
											//query DB for user	
											Any rv2 = ORB.init().create_any();
											// //pioUDAs.println(theSSL2);
											UserInfo[] userinfo=null;
											String userid="";
											try
											{
												rv2.insert_string("User_v300_p::id_number = "+ theSSL2);
												userinfo = myFrame.getUserInfo(rv2);
												userid	=userinfo[0].user_id;

											}
											catch(Exception ex)
											{
												userid="User "+theSSL2+" deleted";
											}


											//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + userinfo[0].user_id+";");
											String almlink="<A href=''"+"";
											//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
											if(baselinename.equalsIgnoreCase("Current Baseline"))
											{

												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

											}
											else
											{
												almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

											}


											String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(userid)  +"','"+almlink+ "','','"+updatetime+"')";
											////System.out.println (SQLinsert);
											stmt.addBatch(SQLinsert);
											//con.commit();

										}
										if(thetype.value()==6) //for SSGL
										{
											//query db for group
											// //pioUDAs.println(theSSL2);
											Any rv2 = ORB.init().create_any();
											rv2.insert_string("Group_v300_p::id_number = " + theSSL2);
											GroupInfo[] grpinfo = myFrame.getGroupInfo(rv2);
											//grpinfo[0].name	
											if (grpinfo.length==0)
											{
												//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + ""+";");
												String almlink="<A href=''"+"";
												//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
												if(baselinename.equalsIgnoreCase("Current Baseline"))
												{

													almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

												}
												else
												{
													almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

												}
												String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + ""  +"','"+almlink+ "','','"+updatetime+"')";
												////System.out.println (SQLinsert);
												stmt.addBatch(SQLinsert);
												//con.commit();
											}
											else
											{
												//pioUDAs.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + versions[v].major_version_number + "." + versions[v].minor_version_number+";"+ UDA_type+";"+UDA_info[0].uda_info.name+";"+reqinfoV.uda_values[u].uda_id_number + ";" + grpinfo[0].name+";");
												String almlink="<A href=''"+"";
												//alm://caliberrm!bel-davidca.microfocus.com_20000_105/1858;ns=requirement?baseline=164
												if(baselinename.equalsIgnoreCase("Current Baseline"))
												{

													almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement'"+"'>Req:"+reqid+"</A>";

												}
												else
												{
													almlink="<A href=''"+"alm://caliberrm!"+ Hostname+"_20000_" + projectID+"/"+ reqid + ";ns=requirement?baseline=" + baselineID +"''>Req:"+reqid+"</A>";

												}
												String SQLinsert = "INSERT INTO REQ_INFO  VALUES (" + projectID+","+ baselineID+",'"+ LocaliseString(reqtype)+"'"+","+Depth+","+reqid + ",'" + treeversion + "'," + UDA_type +",'"+LocaliseString(UDA_info[0].uda_info.name)+"',"+reqinfoV.uda_values[u].uda_id_number+",'" + LocaliseString(grpinfo[0].name)  +"','"+almlink+ "','','"+updatetime+"')";
												////System.out.println (SQLinsert);
												stmt.addBatch(SQLinsert);
												//con.commit();

											}
										}
									}
								}
							}

							catch(Exception e)
							{
								pioErrors.println(projectID+";"+ baselineID+",'"+ reqtype+"'"+";"+"0"+";"+reqid + ";" + treeversion + ";" + "1" +";"+UDA_info[0].uda_info.name+";" + e.getLocalizedMessage());

							}
							fintest = System.currentTimeMillis();
							//System.out.println("UDAs took" + (fintest-strttest));
						}//udavalues
						//}
					}

					catch(Exception ex){}

					//}//for Level

				}
				catch(Exception reqex)
				{
					System.out.println(reqex.getMessage());
				}
			}//if proj baseline


			//}//for UPDATE List
			stmt.executeBatch();

			con.commit();
		} 
		catch (Exception e1) 
		{
			if(e1.getMessage().contains("OBJECT_DOES_NOT_EXIST"))
			{
				errorpoint="reqinfo OBJECT_DOES_NOT_EXIST";
				//list contain a none requirement object
			}
			else{
				System.out.println("DELETE Create SQL Err:"+e1.getMessage());
				System.out.println("error point:"+errorpoint );
			}
		}
		Thread.currentThread().isAlive();
		Thread.currentThread().run();
		return true;
	}


	public static String LocaliseString(String Value)
	{

		try
		{
			try
			{
			String theStartchar=Value.substring(0, 1);
			if(theStartchar.equalsIgnoreCase("'"))
			{
				Value=Value.substring(1);
			}
			int last =Value.length()-1;
			String theEndchar=Value.substring(last, last+1);
			if(theEndchar.equalsIgnoreCase("'"))
			{
				Value=Value.substring(0,Value.length()-1);
			}
			}
			catch(Exception e){}
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
			System.out.println("Encode error "+ Value + " "+ ex.getMessage());
		}
	
		Value = Value.replaceAll("'", "''");
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

	public static String StarTeamInfo(String StarteamConnnectionInfo)//, Session session, int req,int baseid,int traceid,boolean Tracesuspect, Statement stmt)
	{

		String returnString="";
		//login to starteam
		//String strAddress = "";
		//int nPort = -1;
		String teststring ="";
		try{
			String strUserName = starteamlogin; 
			String strPassword = starteampass;

			String[] Ststring = StarteamConnnectionInfo.split("[:]");
			StarteamConnnectionInfo=StarteamConnnectionInfo.replace("\t\t\t", ":");
			if(!StarteamConnnectionInfo.contains("Backlog"))
			{		
				for(int st=0; st<Ststring.length;st++)
				{
					if(st==0)
					{
						String[] genType_server = Ststring[st].split("			");
						strAddress=genType_server[1];
					}
					else if(st==1)
					{
						nPort=Integer.parseInt(Ststring[st]);
					}
					else
					{
						System.out.println ("artefact:" + Ststring[st]);
					}
				}
				projID= Integer.parseInt(Ststring[Ststring.length-4]);
				viewID= Integer.parseInt(Ststring[Ststring.length-3]);
				teststring = Ststring[Ststring.length-2];
			}
			else
			{
				//assumption, last connection is correct
				//obtain st story id
				teststring="Story";
			}
			// Create a new StarTeam Server object. 

			Server s = new Server(strAddress, nPort);
			s.connect(); 

			// LogOn as a specific user. 

			s.logOn(strUserName, strPassword);
			//System.out.println ("logged on to StarTeam: "+ strAddress + " " +  nPort + " " + s.isConnected());
			TraceManager tm = session.getTraceManager();
			//need to get if trace is suspect and last modified date
			String moddate="";





			com.starteam.Project stproj = null;
			stproj=s.findProject(projID);


			//stproj = s.getProjects()[projID];
			//System.out.println ("Project ID: "+ projID + " Name: " + stproj.getName());

			View view = stproj.findView(viewID);



			TypeCollection mytype = s.getTypes();
			Type chgreqType=(Type) mytype.find("ChangeRequest");
			Type reqType=(Type) mytype.find("Requirement");
			Type topicType=(Type) mytype.find("Topic");
			Type fileType=(Type) mytype.find("File");
			Type taskType=(Type) mytype.find("Task");
			Type storyType=(Type) mytype.find("Story");


			for(int st=0; st<mytype.size();st++)
			{
				System.out.println (mytype.get(st));

			}

			int itemID=Integer.parseInt(Ststring[Ststring.length-1]);

			try
			{
				if(teststring.equalsIgnoreCase("ChangeRequest"))
				{
					System.out.println ("Project Name: " + stproj.getName() +" View Name: "+view.getName());
					Item fst = view.findItem(chgreqType, itemID);
					PropertyCollection props = fst.getDisplayableProperties();
					String propString="";
					String Status = "";
					for(int p=0; p<props.size();p++)
					{
						propString=propString + "," +  props.get(p);

						if(props.get(p).toString().equals("Status"))
						{	
							Status=fst.getStringValue((Property) props.get(p));
							System.out.println ("ChangeRequest:"+Status);
						}
					}
					//System.out.println ("Change Request: "+fst.getDisplayName() + " " + fst.getDisplayValue(props.find("Synopsis"))+ " " + fst.getCreatedTime() + " " + fst.getModifiedTime() + " " + fst.getModifiedBy().getName());
					long epoch=fst.getCreatedTime().toJavaMsec()/1000;
					String created_date= new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					epoch=fst.getModifiedTime().toJavaMsec()/1000;

					String mod_date =new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					//if modified date is different set trace supspect
					// add status

					returnString = StarteamConnnectionInfo + "','" + "Change Request" + "','" + fst.getDisplayName() + "','" + fst.getDisplayValue(props.find("Synopsis")) + "','" + created_date + "','" + mod_date + "','" + fst.getModifiedBy().getName() + "','" + Status;
					//System.out.println (propString);
				}
				if(teststring.equalsIgnoreCase("Topic"))
				{
					System.out.println ("Project Name: " + stproj.getName() +" View Name: "+view.getName());
					Item fst = view.findItem(topicType, itemID);
					PropertyCollection props = fst.getDisplayableProperties();
					String propString="";
					String Status = "";
					for(int p=0; p<props.size();p++)
					{
						propString=propString + "," +  props.get(p);
						//System.out.println ("Topic:"+props.get(p).toString());
						if(props.get(p).toString().equals("Status"))
						{
							Status=fst.getStringValue((Property) props.get(p));
							System.out.println ("Topic:"+Status);
						}
					}
					//System.out.println ("Topic: "+fst.getDisplayName() + " "+ fst.getDisplayValue(props.find("Title"))+" " + fst.getCreatedTime() + " " + fst.getModifiedTime() + " " + fst.getModifiedBy().getName());
					long epoch=fst.getCreatedTime().toJavaMsec()/1000;
					String created_date= new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					epoch=fst.getModifiedTime().toJavaMsec()/1000;
					String mod_date =new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					//if modified date is different set trace supspect
					// add status

					returnString = StarteamConnnectionInfo + "','" + "Topic" + "','" + fst.getDisplayName() + "','" + fst.getDisplayValue(props.find("Title")) + "','" + created_date + "','" + mod_date + "','" + fst.getModifiedBy().getName() + "','" + Status;

					//System.out.println (propString);
				}
				if(teststring.equalsIgnoreCase("File"))
				{
					System.out.println ("Project Name: " + stproj.getName() +" View Name: "+view.getName());
					Item fst = view.findItem(fileType, itemID);
					PropertyCollection props = fst.getDisplayableProperties();
					String propString="";
					for(int p=0; p<props.size();p++)
					{
						//Status=fst.getStringValue((Property) props.get(p));
						//System.out.println ("ChangeRequest:"+Status);
					}

					String File_name = fst.getDisplayName().replace("\"", "");
					File_name = fst.getDisplayName().replace("File ", "");

					//System.out.println ("File: "+File_name + " " + fst.getCreatedTime() + " " + fst.getModifiedTime() + " " + fst.getModifiedBy().getName());
					long epoch=fst.getCreatedTime().toJavaMsec()/1000;
					String created_date= new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					epoch=fst.getModifiedTime().toJavaMsec()/1000;
					String mod_date =new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					//need to add status - "Current" if not suspect and modified date is the same
					//or "Modified" if modified date is different - set trace suspect
					//or "Modified" if modified date is the same and trace suspect
					String Status = "New";



					returnString = StarteamConnnectionInfo + "','" + "File" + "','" + File_name + "','" + fst.getDisplayValue(props.find("Description")) + "','" + created_date + "','" + mod_date + "','" + fst.getModifiedBy().getName() +  "','" + Status;

					//System.out.println (propString);
				}
				if(teststring.equalsIgnoreCase("Requirement"))
				{
					System.out.println ("Project Name: " + stproj.getName() +" View Name: "+view.getName());
					Item fst = view.findItem(reqType, itemID);
					PropertyCollection props = fst.getDisplayableProperties();
					String propString="";
					String Status = "";
					for(int p=0; p<props.size();p++)
					{
						propString=propString + "," +  props.get(p);
						//System.out.println ("Requirement:"+props.get(p).toString());
						if(props.get(p).toString().equals("Status"))
						{
							Status=fst.getStringValue((Property) props.get(p));
							System.out.println ("Requirement:"+Status);
						}
					}
					//System.out.println ("Requirement: "+fst.getDisplayName() + " " + fst.getDisplayValue(props.find("Name"))+" "+ fst.getCreatedTime() + " " + fst.getModifiedTime() + " " + fst.getModifiedBy().getName());
					long epoch=fst.getCreatedTime().toJavaMsec()/1000;
					String created_date= new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					epoch=fst.getModifiedTime().toJavaMsec()/1000;
					String mod_date =new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					//if modified date is different set trace supspect
					// add status
					Item streqs = view.findItem(mytype.REQUIREMENT, itemID);
					TraceCollection tracesSTobj = streqs.getTraces(true);

					for(int st=0; st<tracesSTobj.size();st++)
					{

						com.starteam.Trace ststorytrace = (com.starteam.Trace) tracesSTobj.get(st);
						LinkValue target = ststorytrace.getTarget();
						System.out.println (target.getType());
						System.out.println (target.getVMID());
						int stid=target.getVMID();

					}


					returnString = StarteamConnnectionInfo + "','" + "Requirement" + "','" + fst.getDisplayName() + "','" + fst.getDisplayValue(props.find("Name")) + "','" + created_date + "','" + mod_date + "','" + fst.getModifiedBy().getName() +  "','" + Status;

					//System.out.println (propString);
				}

				if(teststring.equalsIgnoreCase("Task"))
				{
					System.out.println ("Project Name: " + stproj.getName() +" View Name: "+view.getName());
					Item fst = view.findItem(taskType, itemID);
					PropertyCollection props = fst.getDisplayableProperties();
					String propString="";
					String Status = "";
					for(int p=0; p<props.size();p++)
					{
						propString=propString + "," +  props.get(p);
						//System.out.println ("Task:"+props.get(p).toString());
						if(props.get(p).toString().equals("StTaskStatus"))
						{
							Status=fst.getStringValue((Property) props.get(p));
							System.out.println ("Task:"+Status);
						}
					}
					//System.out.println ("Task: "+fst.getDisplayName() + " " + fst.getDisplayValue(props.find("StTaskName"))+" " + fst.getCreatedTime() + " " + fst.getModifiedTime() + " " + fst.getModifiedBy().getName());
					long epoch=fst.getCreatedTime().toJavaMsec()/1000;
					String created_date= new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					epoch=fst.getModifiedTime().toJavaMsec()/1000;
					String mod_date =new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					//if modified date is different set trace supspect
					// add status

					returnString = StarteamConnnectionInfo + "','" + "Task" + "','" + fst.getDisplayName() + "','" + fst.getDisplayValue(props.find("StTaskName")) + "','" + created_date + "','" + mod_date + "','" + fst.getModifiedBy().getName() +  "','" + Status;

					//System.out.println (propString);
				}

				if(teststring.equalsIgnoreCase("Requirement"))
				{
					//this is to get the story for the requirement

					System.out.println ("Project Name: " + stproj.getName() +" View Name: "+view.getName());
					Item fst = view.findItem(reqType, itemID);
					if(itemID==1470)
					{
						System.out.println ("Hello");
					}

					PropertyCollection props = fst.getDisplayableProperties();
					String propString="";
					String Status = "";
					for(int p=0; p<props.size();p++)
					{
						propString=propString + "," +  props.get(p);
						//System.out.println ("Requirement:"+props.get(p).toString());
						if(props.get(p).toString().equals("Status"))
						{
							Status=fst.getStringValue((Property) props.get(p));
							System.out.println ("Requirement:"+Status);
						}
					}
					//System.out.println ("Requirement: "+fst.getDisplayName() + " " + fst.getDisplayValue(props.find("Name"))+" "+ fst.getCreatedTime() + " " + fst.getModifiedTime() + " " + fst.getModifiedBy().getName());
					long epoch=fst.getCreatedTime().toJavaMsec()/1000;
					String created_date= new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					epoch=fst.getModifiedTime().toJavaMsec()/1000;
					String mod_date =new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new java.util.Date (epoch*1000));
					//if modified date is different set trace supspect
					// add status

					returnString = StarteamConnnectionInfo + "','" + "Requirement" + "','" + fst.getDisplayName() + "','" + fst.getDisplayValue(props.find("Name")) + "','" + created_date + "','" + mod_date + "','" + fst.getModifiedBy().getName() +  "','" + Status;

					//System.out.println (propString);
				}

				//					if(teststring.equalsIgnoreCase("Trace"))
				//					{
				//						System.out.println ("---->Trace");
				//
				//					}
				//					if(teststring.equalsIgnoreCase("Change Package"))
				//					{
				//						System.out.println ("---->Change Package");
				//
				//					}
				//					if(teststring.equalsIgnoreCase("Item Revision"))
				//					{
				//						System.out.println ("---->Item Revision");
				//
				//					}
				//					if(teststring.equalsIgnoreCase("WorkRecord"))
				//					{
				//						System.out.println ("---->WorkRecord");
				//
				//					}
				//					if(teststring.equalsIgnoreCase("Audit"))
				//					{
				//						System.out.println ("---->Audit");
				//
				//					}
				//					if(teststring.equalsIgnoreCase("UserObject"))
				//					{
				//						System.out.println ("---->UserObject");
				//
				//					}
				//					if(teststring.equalsIgnoreCase("GroupObject"))
				//					{
				//						System.out.println ("---->GroupObject");
				//
				//					}
				//					
				//					if(teststring.equalsIgnoreCase("Item Reference"))
				//					{
				//						System.out.println ("---->Item Reference");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}
				//					
				//					if(teststring.equalsIgnoreCase("PromotionModel"))
				//					{
				//						System.out.println ("---->PromotionModel");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}
				//					if(teststring.equalsIgnoreCase("ConfigLabel"))
				//					{
				//						System.out.println ("---->ConfigLabel");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}
				//					
				//					if(teststring.equalsIgnoreCase("Change"))
				//					{
				//						System.out.println ("---->Change");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}
				//					if(teststring.equalsIgnoreCase("View"))
				//					{
				//						System.out.println ("---->View");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}
				//					if(teststring.equalsIgnoreCase("Folder"))
				//					{
				//						System.out.println ("---->Folder");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}
				//					if(teststring.equalsIgnoreCase("PromotionState"))
				//					{
				//						System.out.println ("---->PromotionState");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}
				//					if(teststring.equalsIgnoreCase("Dependencies"))
				//					{
				//						System.out.println ("---->Dependencies");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}
				//					
				//					
				//					if(teststring.equalsIgnoreCase("Project"))
				//					{
				//						System.out.println ("---->Project");
				//						//System.out.println (theType+": "+fst.getDisplayName());
				//					}

			}
			catch(Exception e)
			{
				System.out.println("Error:" + e.getMessage());
			}

			s.disconnect(); 

			//System.out.println ("logged out to StarTeam: "+ strAddress + " " +  nPort + " " + s.isConnected());
		}
		catch(Exception e)
		{
			System.out.println("Error:" + e.getMessage());
		}
		return returnString;

	}


	public static String QCInfo(String QCConnnectionInfo)
	{
		String ReturnString = "'','','','','','',''";
		if(QCYES.equalsIgnoreCase("QCYES"))
		{

			Connection con = null;
			Statement stmt = null;
			Statement stmt2 = null;
			ResultSet rs = null;
			String type="";
			String URL="";
			String domain ="";
			String QCProject ="";
			String objID="";
			String SQL= "";
			String[] split1 = QCConnnectionInfo.split("\\\\");
			//Quality Center			REQ, http://10.150.14.20:8080/qcbin, DEFAULT, WarrenTest, 13
			// Establish the connection.



			for (int i=0; i<split1.length;i++)
			{
				//System.out.println(i + " " + split1[i]);
			}
			type=split1[0];
			URL=split1[1];
			domain=split1[2];
			QCProject=split1[3];
			objID=split1[4];
			String dbinstance = domain+"_"+QCProject+"_db";

			try
			{
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
				//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
				try{

					con = DriverManager.getConnection("jdbc:sqlserver://"+QCDB+";DataBaseName="+dbinstance+";user="+QCDBuser+";password="+QCDBpassword);


					//System.out.println("Created Driver");	
					//stmt = con.createStatement();
					stmt2 = con.createStatement();
					//System.out.println("Created");
				}
				catch(Exception Ex)
				{
					//System.out.println("Issue:" + Ex.getMessage()+ " " + Ex.getStackTrace().length);
				}



				if(type.contains("TEST"))
				{
					SQL= "Select TS_TEST_ID,TS_Name,TS_Status,TS_Creation_Date,TS_TYPE,TS_EXEC_STATUS,TS_VTS FROM [td].TEST WHERE TS_TEST_ID=" + objID;
					//System.out.println(SQL);
					try
					{
						ResultSet result = stmt.executeQuery(SQL);
						while(result.next())
						{
							String QC_Type = type;//result.getString("TS_TYPE");
							String QC_ID = result.getString("TS_TEST_ID");
							String QC_Name = result.getString("TS_Name");
							String Exe_Status = result.getString("TS_EXEC_STATUS");
							//get TS_VTS date from VC_TEST
							String Modified ="";
							String SQLMod="Select TS_VTS FROM td.VC_TEST WHERE TS_TEST_ID="+ objID;
							try{
								ResultSet result2 = stmt2.executeQuery(SQLMod);
								while(result2.next())
								{
									Modified = result2.getString("TS_VTS");
								}
								result2.close();
							}
							catch(Exception ex){}

							String Created = result.getString("TS_Creation_Date");
							//System.out.println(result.getString("TS_EXEC_STATUS"));
							ReturnString="'"+domain+" "+ QCProject+ "','"+ QC_Type + "','" + QC_ID + "','" +  QC_Name + "','" +  Exe_Status + "','" +  Created + "','" +  Modified  + "'";;

						}
						result.close();
					}
					catch(Exception Ex)
					{
						System.out.println("Issue:" + Ex.getMessage()+ " " + Ex.getStackTrace().length);
					}
				}
				if(type.contains("REQ"))
				{
					SQL= "Select RQ_REQ_ID,RQ_REQ_TYPE,RQ_REQ_PRODUCT,RQ_REQ_Name,RQ_REQ_Date,RQ_REQ_TIME,RQ_VTS,RQ_REQ_STATUS FROM [td].REQ WHERE RQ_REQ_ID=" + objID;
					//System.out.println(SQL);
					try
					{
						ResultSet result = stmt.executeQuery(SQL);
						while(result.next())
						{
							String QC_Type = type;//result.getString("RQ_REQ_TYPE");
							String QC_ID = result.getString("RQ_REQ_ID");
							String QC_Name = result.getString("RQ_REQ_Name");
							String Exe_Status = result.getString("RQ_REQ_STATUS");
							String thedate =result.getString("RQ_REQ_DATE");
							String thetime =result.getString("RQ_REQ_TIME");

							String Created = thedate.split(" ")[0] + " " +thetime;
							String Modified ="";
							String SQLMod="Select RQ_VTS FROM td.VC_REQ WHERE RQ_REQ_ID="+ objID;
							try{
								ResultSet result2 = stmt2.executeQuery(SQLMod);
								while(result2.next())
								{
									Modified = result2.getString("RQ_VTS");
								}
								result2.close();
							}
							catch(Exception ex){}


							//System.out.println(result.getString("RQ_REQ_STATUS"));
							ReturnString="'"+domain+" "+ QCProject+ "','"+ QC_Type + "','" + QC_ID + "','" +  QC_Name + "','" +  Exe_Status + "','" +  Created + "','" +  Modified + "'";
						}
						result.close();
					}
					catch(Exception Ex)
					{
						System.out.println("Issue:" + Ex.getMessage()+ " " + Ex.getStackTrace().length);
					}
				}
				if(type.contains("STEP"))
				{
					SQL= "Select DS_ID,DS_STEP_Name,DS_VTS,DS_TEST_ID FROM [td].VC_DESSTEPS WHERE DS_ID=" + split1[5];
					//System.out.println(SQL);
					try
					{
						ResultSet result = stmt.executeQuery(SQL);
						while(result.next())
						{
							String QC_Type = type;//result.getString("TS_TYPE");
							String QC_ID = result.getString("DS_ID");
							String QC_Name = result.getString("DS_STEP_Name");
							//exe status is the test execution status
							int testid=result.getInt("DS_TEST_ID");
							String Exe_Status = "";
							String SQLT= "Select TS_EXEC_STATUS FROM [td].TEST WHERE TS_TEST_ID=" + testid;
							ResultSet resultT = stmt2.executeQuery(SQLT);
							while(resultT.next())
							{
								Exe_Status =resultT.getString("TS_EXEC_STATUS");
							}
							//get TS_VTS date from VC_TEST
							String Modified =result.getString("DS_VTS");



							String Created = result.getString("DS_VTS");
							//System.out.println(result.getString("TS_EXEC_STATUS"));
							ReturnString="'"+domain+" "+ QCProject+ "','"+ QC_Type + "','" + QC_ID + "','" +  QC_Name + "','" +  Exe_Status + "','" +  Created + "','" +  Modified  + "'";;

						}
						result.close();
					}
					catch(Exception Ex)
					{
						System.out.println("Issue:" + Ex.getMessage()+ " " + Ex.getStackTrace().length);
					}
				}
				if(type.contains("SET"))
				{
					SQL= "Select CY_CYCLE_ID,CY_CYCLE,CY_OPEN_DATE,CY_STATUS,CY_VTS FROM [td].CYCLE WHERE CY_CYCLE_ID=" + objID;
					//System.out.println(SQL);
					try
					{
						ResultSet result = stmt.executeQuery(SQL);
						while(result.next())
						{
							String QC_Type = type;//result.getString("TS_TYPE");
							String QC_ID = result.getString("CY_CYCLE_ID");
							String QC_Name = result.getString("CY_CYCLE");
							String Exe_Status = result.getString("CY_STATUS");

							String Modified = result.getString("CY_VTS");

							String Created = result.getString("CY_OPEN_DATE");

							ReturnString="'"+domain+" "+ QCProject+ "','"+ QC_Type + "','" + QC_ID + "','" +  QC_Name + "','" +  Exe_Status + "','" +  Created + "','" +  Modified+"'";

						}
						result.close();
					}
					catch(Exception Ex)
					{
						System.out.println("Issue:" + Ex.getMessage()+ " " + Ex.getStackTrace().length);
					}
				}
				con.close();
			}
			catch(Exception Ex)
			{
				//System.out.println("Issue:" + Ex.getMessage()+ " " + Ex.getStackTrace().length);
			}
			//sql = "CREATE TABLE QC_Traces(ProjectID INTEGER,BaselineID INTEGER,Req_ID INTEGER,Version VARCHAR(128),QC_Project VARCHAR(128),QC_Type VARCHAR(128),QC_ID,QC_Name VARCHAR(128),Exe_Status VARCHAR(128),Created VARCHAR(128),Modified VARCHAR(128),Suspect VARCHAR(5))";
		}
		return ReturnString;

	}


	public static String ReturnCreationDate(int reqid)
	{
		long CreationDate = 0;
		try{

			Predicate r = session2.newAttrInt("Requirement_v300_p::id_number").eq(reqid);

			ClassHandle cls = session2.locateClass ("Requirement_v300_p");

			HandleEnumeration e = cls.select (r);
			HandleVector vector = session2.newHandleVector (e);

			for (int i = 0; i < vector.size(); i++) 
			{

				Handle handle = vector.handleAt (i);

				Predicate rv = session2.newAttrInt("CaliberBase_p::id_number").eq(reqid);
				ClassHandle cls2 = session2.locateClass ("RequirementAttributes_v900_p");
				HandleEnumeration e2 = cls2.select (rv);
				HandleVector vector2 = session2.newHandleVector (e2);


				//delete all changerecords_p and changes_p
				AttrHandleArray ReqChanges = session2.newAttrHandleArray("Requirement_v300_p::revision_history.VEList<Link<PObject>>::elements");
				Handle[] chgRecordahndle = handle.get(ReqChanges);
				for(int CR=0; CR<chgRecordahndle.length;CR++)
				{

					//AttrHandleArray Changes = session.newAttrHandleArray("ChangeRecord_p::change_list.VEList<Link<PObject>>::elements");
					AttrInt timesec = session2.newAttrInt("ChangeRecord_p::date_time.VTime::sec");
					AttrInt majvers = session2.newAttrInt("ChangeRecord_p::major_version_number");
					AttrInt minvers = session2.newAttrInt("ChangeRecord_p::minor_version_number");
					try
					{
						//					System.out.println(chgRecordahndle[CR].get(majvers));
						//					System.out.println(chgRecordahndle[CR].get(minvers));
						//					System.out.println(chgRecordahndle[CR].get(timesec));
						int maj=chgRecordahndle[CR].get(majvers);
						int min = chgRecordahndle[CR].get(minvers);
						if(maj==1 && min ==0)
						{
							CreationDate=chgRecordahndle[CR].get(timesec);
							break;
						}
					}
					catch(Exception e1)
					{
						System.out.println(e1.getMessage());
					}

				}	

			}
		}
		catch(Exception e)
		{
			System.out.println("Creation date:"+e.getMessage());
		}
		String creationdate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (CreationDate*1000));

		return creationdate;

	}

	public static String ReturnModDate(int reqid,int major, int minor)
	{	


		long ModDate = 0;
		long revisioncount =0;

		try
		{
			Predicate r = session2.newAttrInt("Requirement_v300_p::id_number").eq(reqid);

			ClassHandle cls = session2.locateClass ("Requirement_v300_p");
			HandleEnumeration e = cls.select (r);
			HandleVector vector = session2.newHandleVector (e);
			for (int i = 0; i < vector.size(); i++) 
			{

				Handle handle = vector.handleAt (i);

				Predicate rv = session2.newAttrInt("CaliberBase_p::id_number").eq(reqid);
				ClassHandle cls2 = session2.locateClass ("RequirementAttributes_v900_p");
				HandleEnumeration e2 = cls2.select (rv);
				HandleVector vector2 = session2.newHandleVector (e2);


				//delete all changerecords_p and changes_p
				AttrHandleArray ReqChanges = session2.newAttrHandleArray("Requirement_v300_p::revision_history.VEList<Link<PObject>>::elements");
				Handle[] chgRecordahndle = handle.get(ReqChanges);
				for(int CR=0; CR<chgRecordahndle.length;CR++)
				{

					//AttrHandleArray Changes = session.newAttrHandleArray("ChangeRecord_p::change_list.VEList<Link<PObject>>::elements");
					AttrInt timesec = session2.newAttrInt("ChangeRecord_p::date_time.VTime::sec");
					AttrInt majvers = session2.newAttrInt("ChangeRecord_p::major_version_number");
					AttrInt minvers = session2.newAttrInt("ChangeRecord_p::minor_version_number");
					try
					{
						//					System.out.println(chgRecordahndle[CR].get(majvers));
						//					System.out.println(chgRecordahndle[CR].get(minvers));
						//					System.out.println(chgRecordahndle[CR].get(timesec));
						int maj=chgRecordahndle[CR].get(majvers);
						int min = chgRecordahndle[CR].get(minvers);
						revisioncount++;
						if(maj==major && min ==minor)
						{
							ModDate=chgRecordahndle[CR].get(timesec);

							break;
						}
					}
					catch(Exception e1)
					{
						System.out.println(e1.getMessage());
					}

				}	

			}
		}
		catch(Exception ex)
		{
			System.out.println("mod date issue"+ex.getMessage());
		}
		String moddate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (ModDate*1000));
		return moddate + "|" + revisioncount;

	}

	public static String GetTraceInfo(int traceid)
	{	
		String SQLinsert ="";

		//"SELECT * FROM Trace_v900_p"
		Predicate r = session2.newAttrInt("Trace_v900_p::id_number").eq(traceid);

		ClassHandle cls = session2.locateClass ("Trace_v900_p");
		HandleEnumeration e = cls.select (r);
		HandleVector vector = session2.newHandleVector (e);
		AttrHandle from = session2.newAttrHandle("Trace_v900_p::from_object");
		AttrHandle to = session2.newAttrHandle("Trace_v900_p::to_object");
		AttrBoolean suspect = session2.newAttrBoolean("Trace_v900_p::suspect");

		AttrInt reqid = session2.newAttrInt("CaliberBase_p::id_number");

		AttrInt major = session2.newAttrInt("RequirementAttributes_v900_p::major_version_number");
		AttrInt minor = session2.newAttrInt("RequirementAttributes_v900_p::minor_version_number");

		for (int i = 0; i < vector.size(); i++) 
		{
			try
			{
				Handle handle = vector.handleAt (i);

				Handle reqfrom = handle.get(from);
				int reqfromid = reqfrom.get(reqid);
				if(reqfromid==277)
				{
					System.out.println ("Hello");
				}
				int reqfrommajor = reqfrom.get(major);
				int reqfromminor = reqfrom.get(minor);
				String VersionFrom = reqfrommajor+"." + reqfromminor;

				Handle toreq = handle.get(to);
				int reqtoid = toreq.get(reqid);
				int reqtomajor = toreq.get(major);
				int retominor = toreq.get(minor);

				String VersionTo = reqtomajor + "." + retominor;

				boolean suspectvalue = handle.get(suspect);
				SQLinsert = "INSERT INTO Trace_info VALUES (" + reqfromid + ",'" +VersionFrom + "','TO',"  +reqtoid + ",'" + VersionTo+ "','" + suspectvalue +"')";
				//System.out.println (SQLinsert);

			}
			catch(Exception e1)
			{
				System.out.println(e1.getMessage());
			}

		}	


		return SQLinsert;

	}

	
	/*public static String encodeHTMLmages(String html) 
	{
		//find images string and replace it with encoded data
		Pattern p = Pattern.compile("<img "+"(.*?)"+">");
		Matcher m = p.matcher(html);

		while(m.find()) 
		{

			String img = m.group();
			//System.out.println (img);
			Pattern p2 = Pattern.compile("src=\""+"(.*?)"+"\"");
			Matcher m2 = p2.matcher(img);
			while(m2.find()) 
			{
				String src = m2.group();
				String orgsrc=src;
				//System.out.println (src);
				String currentHome = System.getProperty("user.home");
				String srctemp =src;
				currentHome=currentHome+"\\.caliberrm\\";
				srctemp=srctemp.replaceAll("\"", "");
				srctemp=src.replaceAll("src=", "");
				srctemp="src=\""+currentHome+srctemp;
				
				
				
				String temp = srctemp.replace("src=\"", "");
				temp = temp.replace("\"", "");
				temp = temp.replace("///", "");
				//find extension
				int lenofstr=temp.length();
				String typeImage=temp.substring(lenofstr-3);
				File theimagefile= new File(temp);
				String encodedimagedata= encodeToString(theimagefile, typeImage);
				html=html.replaceAll(src, "src=\"" + encodedimagedata + "\"");

				String currentHome = System.getProperty("user.home");
                                        String currentUser = System.getProperty("user.name");
                                        currentHome=currentHome.replaceAll("\\\\", "\\\\\\\\");
                                        String replacetext="src=\""+currentHome+"\\\\.caliberrm\\\\";
                                        src=src.replaceAll("src=\"", "");
                                        src=replacetext+src;
                                        String newString = src.replaceAll("src", "new");
                                        src = src + " " + newString;
                                        System.out.println (src);
                                        HTML=HTML.replaceAll(orgsrc, src);

			}

		}

		//System.out.println (html);
		return html;

	}*/
	
	public static String encodeToString(File Thefile, String type) {

		String imageString = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			BufferedImage image=ImageIO.read(Thefile);
			ImageIO.write(image, type, bos);
			byte[] imageBytes = bos.toByteArray();
			BASE64Encoder encoder = new BASE64Encoder();
			imageString = encoder.encode(imageBytes);
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "data:image/png;base64,"+imageString;
	}
}


