package com.fusionjack.adhell3.service;

import com.fusionjack.adhell3.IShizukuService;
import com.fusionjack.adhell3.utils.LogUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

//To debug attach to different process defined in Shizuku.UserServiceArgs (check usages)
public class ShizukuService extends IShizukuService.Stub {
    public ShizukuService() {
        LogUtils.info("Started ShizukuService");
    }

    @Override
    public void destroy() {
        LogUtils.info("Killed ShizukuService");
        System.exit(0);
    }

    @Override
    public String execute(String command) {
        LogUtils.info("ShizukuService Executing command: " + command);
        Process process;
        StringBuilder result = new StringBuilder();
        try {
            process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                result.append("Error (").append(exitCode).append(") \n");
            }

            BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (outputReader.ready()) {
                result.append(outputReader.readLine());
            }

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while (errorReader.ready()) {
                result.append(errorReader.readLine());
            }
        } catch (Exception e) {
            LogUtils.error("ShizukuService execute exception", e);
            return e.toString();
        }
        return result.toString();
    }


}
