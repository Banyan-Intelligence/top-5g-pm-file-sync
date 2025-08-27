package com.tejas.pmfilesync5g.service;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SftpService {

    private static final Pattern SFTP_PATTERN = Pattern.compile("sftp://([^:]+):([^@]+)@([^:]+):(\\d+)(.+)");
    
    public List<String> readTarFileContents(String sftpLocation) throws Exception {
        URI uri = URI.create(sftpLocation);
        
        String username = null;
        String password = null;
        String host = uri.getHost();
        int port = uri.getPort();
        String filePath = uri.getPath();
        
        String userInfo = uri.getUserInfo();
        if (userInfo != null && userInfo.contains(":")) {
            String[] credentials = userInfo.split(":");
            username = credentials[0];
            password = credentials[1];
        }
        
        if (port == -1) {
            port = 22;
        }
        
        log.info("Connecting to SFTP server: {}:{} for file: {}", host, port, filePath);
        
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        List<String> fileList = new ArrayList<>();
        
        try {
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            
            // Configure algorithms for compatibility with modern SFTP servers
            session.setConfig("kex", "diffie-hellman-group14-sha256,diffie-hellman-group14-sha1,diffie-hellman-group1-sha1");
            session.setConfig("server_host_key", "ssh-rsa,ssh-dss,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521");
            session.setConfig("cipher.s2c", "aes128-ctr,aes128-cbc,3des-ctr,3des-cbc,blowfish-cbc,aes192-cbc,aes256-cbc");
            session.setConfig("cipher.c2s", "aes128-ctr,aes128-cbc,3des-ctr,3des-cbc,blowfish-cbc,aes192-cbc,aes256-cbc");
            session.setConfig("mac.s2c", "hmac-md5,hmac-sha1,hmac-sha2-256,hmac-sha1-96,hmac-md5-96");
            session.setConfig("mac.c2s", "hmac-md5,hmac-sha1,hmac-sha2-256,hmac-sha1-96,hmac-md5-96");
            session.setConfig("compression.s2c", "none");
            session.setConfig("compression.c2s", "none");
            
            session.setTimeout(30000);
            session.connect();
            
            log.info("Session connected to {}:{}", host, port);
            
            channel = (ChannelExec) session.openChannel("exec");
            String command = "tar -tf " + filePath;
            log.info("Executing command: {}", command);
            channel.setCommand(command);
            
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(err));
            
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("Tar content: {}", line);
                if (line.trim().endsWith(".xml")) {
                    fileList.add(line.trim());
                }
            }
            
            String errLine;
            while ((errLine = errReader.readLine()) != null) {
                log.warn("SFTP command error: {}", errLine);
            }
            
            channel.disconnect();
            log.info("Found {} XML files in tar archive: {}", fileList.size(), filePath);
            
        } catch (Exception e) {
            log.error("Error reading tar file contents from SFTP: {}", sftpLocation, e);
            throw e;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        
        return fileList;
    }
}