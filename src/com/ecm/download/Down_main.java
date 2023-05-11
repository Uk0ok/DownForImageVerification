package com.ecm.download;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Down_main {

    private static Logger log = Logger.getLogger("main");

    public static String DB_DRIVER = null;
	public static String DB_URL = null;
	public static String DB_ID = null;
	public static String DB_PWD = null;
	
	public static String ECM_IP = null; 
	public static int ECM_PORT = 0;
	public static String ECM_ID = null; 
	public static String ECM_PWD = null;
	public static String ECM_GW = null;

    public static String PROC_DOWNBASEDIR = null;
    public static String PROC_TXTPATH = null;
    // public static String PROC_EIDTXTPATH = null;
    // public static String PROC_VIDTXTPATH = null;
    // public static String PROC_FKEYTXTPATH = null;

    public static String SQL_SELECTVOLUMELIST = null;
    public static String SQL_SELECT = null;
    // public static String SQL_SELECTBEFORE = null;
    // public static String SQL_SELECTAFTER = null;
    public static String SQL_UPDATE = null;
    public static String SQL_RECOVERY = null;
    
    public static void main(String[] args) {

        PropertyConfigurator.configure("./conf/log4j.properties");

        log.debug("----- Process Start -----");
        try {
            // Config
            Config.setConfig("./conf/conf.properties");

            DB_DRIVER = Config.getConfig("DB.DRIVER");
            DB_URL = Config.getConfig("DB.URL");
            DB_ID = Config.getConfig("DB.ID");
            DB_PWD = Config.getConfig("DB.PWD");

            ECM_IP = Config.getConfig("ECM.IP");
            ECM_PORT = Config.getIntConfig("ECM.PORT");
            ECM_ID = Config.getConfig("ECM.ID");
            ECM_PWD = Config.getConfig("ECM.PWD");
            ECM_GW = Config.getConfig("ECM.GW");

            PROC_DOWNBASEDIR = Config.getConfig("PROC.DOWNBASEDIR");
            PROC_TXTPATH = Config.getConfig("PROC.TXTPATH");
            // PROC_EIDTXTPATH = Config.getConfig("PROC.EIDTXTPATH");
            // PROC_VIDTXTPATH = Config.getConfig("PROC.VIDTXTPATH");
            // PROC_FKEYTXTPATH = Config.getConfig("PROC.FKEYTXTPATH");

            SQL_SELECTVOLUMELIST = Config.getConfig("SQL.SELECTVOLUMELIST");
            SQL_SELECT = Config.getConfig("SQL.SELECT");
            SQL_UPDATE = Config.getConfig("SQL.UPDATE");
            SQL_RECOVERY = Config.getConfig("SQL.RECOVERY");
            // SQL_SELECTBEFORE = Config.getConfig("SQL.SELECTBEFORE");
            // SQL_SELECTAFTER = Config.getConfig("SQL.SELECTAFTER");

        } catch (IOException e) {
            log.error("Properties Load Error, " + e.getMessage());
            return;
        }

        Down_run dRun = new Down_run();
        try {
            dRun.run();
        } finally {
            dRun.disDBConn();
        }

        log.debug("----- Process End -----");
    }
}
