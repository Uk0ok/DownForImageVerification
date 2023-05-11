package com.ecm.download;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.windfire.apis.asysConnectData;
import com.windfire.apis.asys.asysUsrElement;

public class Down_run {
    private static Logger log = Logger.getLogger("main");

    private Connection conn = null;
    private boolean hasDbConn = true;


    public Down_run() {
        try {
            conn = getConnection(Down_main.DB_URL, Down_main.DB_ID, Down_main.DB_PWD);
        } catch (Exception e) {
            log.error("Down_run constructor Exception : " + e.getMessage());
        }
    }
    // 실행
    public void run() {
        if (hasDbConn) {
            try {
                // load DB class driver
                try {
                    Class.forName(Down_main.DB_DRIVER);
                } catch (ClassNotFoundException e) {
                    log.error("failed to load db driver, " + e.getMessage());
                }  
                // select volume List
                ArrayList<String> volumeList = selectVolumeList();
                if(volumeList != null && volumeList.size() == 0) {
                    log.debug("There's No Volume");
                    return;
                }
                // volume별 elementid 5개씩 추출해서 download && DB값 update후 다운로드
                for (String volumeid : volumeList) {
                        ArrayList<String> elementList = select(volumeid);
                        if(elementList == null || elementList.size() == 0) {
                            log.debug("No Download Target File");
                            return;
                        } else {
                            String filePath = createPath(volumeid);
                            for (String elementid : elementList) {
                                download(filePath, elementid);
                            }
                            // IN(?, ?, ?, ?, ?)
                            update(elementList);
                            System.out.println(elementList);
                            for (String elementNewId : elementList) {
                                download(filePath + "_NEW", elementNewId);
                            }
                            // // 복구 IN(?, ?, ?, ?, ?)
                            recovery(elementList);
                        }
                }
                // 다운로드 한 elementid를 txt파일에 입력
                // writeToFile(elementList);
                // doWork(downlaod + update)
            } catch (Exception e2) {
                log.error("run() error, " + e2.getMessage());
            }
        } else {
            log.error("DB Connection is failure");
        }
    }
    // DBConnection 선언
    public Connection getConnection(String url, String id, String pwd) {
		try {
			conn = DriverManager.getConnection(url, id, pwd);
			conn.setAutoCommit(false);
			
		} catch (SQLException e) {
			log.error("DB Connection error : " + e.getMessage());
            hasDbConn = false;
			return null;
		}
		return conn;
	}
    // create directory
    public String createPath(String vid) {
        
        String filePath = Down_main.PROC_DOWNBASEDIR + "/" + vid.trim();

        File dPath = new File(filePath);
        if(!dPath.exists()) {
            dPath.mkdirs();
        }
        return filePath;
    }
    // download API
    public int download(String filePath, String eid) {
        asysConnectData xconn = new asysConnectData(Down_main.ECM_IP, Down_main.ECM_PORT, "Description", Down_main.ECM_ID, Down_main.ECM_PWD);
        
        File dPath = new File(filePath);
        if(!dPath.exists()) {
            dPath.mkdirs();
        }

        // String elementIdList = eidList.toString();
        try {
            asysUsrElement uePage = new asysUsrElement(xconn);
            uePage.m_elementId = Down_main.ECM_GW + "::" + eid + "::IMAGE";
            Path file = Paths.get(filePath + "/" + eid);
            // Path file = Paths.get(Down_main.PROC_DOWNBASEDIR + "/" + eid);

            int ret = uePage.getContent(file.normalize().toString(), "", "");
            
            if(ret != 0) {
                log.error("Error - download " + uePage.getLastError());
                // System.out.println(uePage.getLastError());
            } else {
                log.debug("Success - download " + uePage.m_elementId);
                // System.out.println(uePage.m_elementId);
            }
    
            return ret;
        } finally {
            if (xconn != null) {
                xconn.close();
            }
        }
    } 
    // VOLUME리스트 SELECT
    public ArrayList<String> selectVolumeList() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList<String> volList = new ArrayList<String>();

        try {
            pstmt = conn.prepareStatement(Down_main.SQL_SELECTVOLUMELIST);
            rs = pstmt.executeQuery();

            while(rs.next()) {
                if(rs.getString("VOLUMEID") == null || rs.getString("VOLUMEID").equals("")) {
                    log.error("[ERROR] there's no VOLUMEID");
                }
                volList.add(rs.getString("VOLUMEID"));
            }
        } catch (SQLException e) {
            log.error("Select Volume List Error, " + e.getMessage());
        } finally {
            try {
                if (rs != null) { rs.close(); }
                if (pstmt != null) { pstmt.close(); }
            } catch (SQLException e) {
                log.error("open cursor close error, " + e.getMessage());
            }
        }
        return volList;
    }
    // 대상 elementid 조회
    public ArrayList<String> select(String vid) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArrayList<String> txList = new ArrayList<String>();

        try {
            pstmt = conn.prepareStatement(Down_main.SQL_SELECT);
            pstmt.setString(1, vid);
            rs = pstmt.executeQuery();

            while(rs.next()) {
                if(rs.getString("ELEMENTID") == null || rs.getString("ELEMENTID").equals("")) {
                    log.error("[ERROR] there's no ELEMENTID");
                }
                txList.add(rs.getString("ELEMENTID"));
            }
        } catch (SQLException e) {
            log.error("Select Download List Error, " + e.getMessage());
        } finally {
            try {
                if (rs != null) { rs.close(); }
                if (pstmt != null) { pstmt.close(); }
            } catch (SQLException e) {
                log.error("open cursor close error, " + e.getMessage());
            }
        }
        return txList;
    }
    // UPDATE ASYSCONTENTELEMENT SET VOLUMEID = TRIM(VOLUMEID) || '_NEW', FILEKEY = REPLACE(FILEKEY, TRIM(VOLUMEID), (TRIM(VOLUMEID) || '_NEW')) WHERE ELEMENTID IN (?, ?, ?, ?, ?);
    public void update(ArrayList<String> eidList) {
        PreparedStatement pstmt = null;

        try {
            pstmt = conn.prepareStatement(Down_main.SQL_UPDATE);
            for(int i = 0; i < eidList.size(); i++) {
                pstmt.setString(i+1, eidList.get(i));
            }
            pstmt.executeUpdate();
            log.debug("Update Storage to Storage_NEW Successed!!");
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to update.." + e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                if (pstmt != null) { pstmt.close(); }
            } catch (SQLException e) {
                log.error("open cursor close error, " + e.getMessage());
            }
        }
    }
    // UPDATE ASYSCONTENTELEMENT SET VOLUMEID = REPLACE(VOLUMEID, '_NEW', ''), FILEKEY = FILEKEY = REPLACE (FILEKEY, '_NEW', '') WHERE ELEMENTID IN (?, ?, ?, ?, ?)
    private void recovery(ArrayList<String> eidList) {
        PreparedStatement pstmt = null;

        try {
            pstmt = conn.prepareStatement(Down_main.SQL_RECOVERY);
            for(int i = 0; i < eidList.size(); i++) {
                pstmt.setString(i+1, eidList.get(i));
            }
            pstmt.executeUpdate();
            log.debug("Update Storage_NEW to Storage Successed!!");
            conn.commit();
        } catch (SQLException e) {
            log.error("Failed to update.." + e.getMessage());
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                if (pstmt != null) { pstmt.close(); }
            } catch (SQLException e) {
                log.error("open cursor close error, " + e.getMessage());
            }
        }
    }
    
    // SELECT한 result를 list파일 경로에 있는 .list파일에 쓰는 프로그램
    public void writeToFile(ArrayList<String> list) {
        try {
            FileWriter fw = new FileWriter(new File(Down_main.PROC_TXTPATH));
            BufferedWriter writer = new BufferedWriter(fw);

            for (String elementid : list) {
                System.out.println(elementid);
                writer.write(elementid + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    // DB disconn
    public void disDBConn() {
        try {
            if (conn != null) { conn.close(); }			
        } catch (Exception e) {
            log.error("Db DisConnect is failure");
        }
    }

    // Update query (fileKey 경로 변경)
    // public void updateQuery(String eid) {
    //     PreparedStatement pstmt = null;
    //     int ret = 0;

    //     try {
    //         pstmt = conn.prepareStatement(Down_main.SQL_UPDATE);
    //         pstmt.setString(1, eid);
    //         ret = pstmt.executeUpdate();

    //         if(ret < 1) {
    //             log.error("[DBUpdate] ExcuteUpdate is Failed, " + eid);
    //         }

    //         conn.commit();
    //     } catch (SQLException e) {
    //         try {
    //             if (conn != null) { conn.rollback(); }

    //         } catch (SQLException e1) {
    //             log.error("Failed to rollback, " + e1.getMessage());
    //         }
    //         log.error("update status Error, " + e.getMessage());
    //     } finally {
    //         try {
    //             if (pstmt != null) { pstmt.close(); }
    //         } catch (SQLException e) {
    //             log.error("open cursor close error, " + e.getMessage());
    //         }
    //     }
    // }
}
