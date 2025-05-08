// IShizukuService.aidl
package com.fusionjack.adhell3;

interface IShizukuService {
    void destroy() = 16777114;

    String execute(String command) = 1;
}