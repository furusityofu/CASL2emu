package jp.ac.fukuoka_u.tl.casl2emu;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

/**
 * Created by furusho on 2016/09/26.
 */

public class Casl2Ftp extends ContextWrapper {
    FTPClient myFTPClient;
        public Casl2Ftp(Context base) {
            super(base);
        }

        public String putData(String remoteserver, int remoteport,
                              String userid, String passwd, boolean passive, String remotefile, String localFile) {
            int reply = 0;
            boolean isLogin = false;
            if(myFTPClient==null) myFTPClient = new FTPClient();
            NTPUDPClient ntpudpClient = new NTPUDPClient();


            try {
                isLogin = login(remoteserver, remoteport, userid, passwd);
                //転送モード
                if (passive) {
                    myFTPClient.enterLocalPassiveMode(); //パッシブモード
                } else {
                    myFTPClient.enterLocalActiveMode();  //アクティブモード
                }//ファイル送信
                myFTPClient.setDataTimeout(15000);
                myFTPClient.setSoTimeout(15000);
                ntpudpClient.open();
                TimeInfo timeInfo = ntpudpClient.getTime(InetAddress.getByName("ntp.nict.jp"));
                Date date = new Date(timeInfo.getReturnTime());
                String s_date = android.text.format.DateFormat.format("yyyyMMddkkmmss",date).toString();
                //FileInputStream fileInputStream = this.openFileInput(localFile);
                FileInputStream fileInputStream = new FileInputStream(new File(localFile));
                myFTPClient.storeFile("~/"+s_date+remotefile.split("/")[1], fileInputStream);
                reply = myFTPClient.getReplyCode();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    throw new Exception("Send Status:" + String.valueOf(reply));
                }
                fileInputStream.close();
                fileInputStream = null;
                //ログアウト
                myFTPClient.logout();
                isLogin = false;
                //切断
                myFTPClient.disconnect();
            } catch (Exception e) {
                return e.getMessage();
            } finally {
                if (isLogin) {
                    try {
                        myFTPClient.logout();
                    } catch (IOException e) {
                    }
                }
                if (myFTPClient.isConnected()) {
                    try {
                        myFTPClient.disconnect();
                    } catch (IOException e) {
                    }
                }
                myFTPClient = null;
                if(ntpudpClient!=null){
                    ntpudpClient.close();
                }
            }
            return null;
        }

    public boolean login(String remoteserver, int remoteport, String userid, String passwd) throws Exception {
        int reply;
        if(myFTPClient==null)
            myFTPClient=new FTPClient();
        myFTPClient.setConnectTimeout(5000);
        //接続
        myFTPClient.connect(remoteserver, remoteport);
        reply = myFTPClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            throw new Exception("Connect Status:" + String.valueOf(reply));
        }
        //ログイン
        if (!myFTPClient.login(userid, passwd)) {
            throw new Exception("Invalid user/password");
        }
        //myFTPClient.logout();
        return true;
    }
}