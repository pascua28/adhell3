# Announcement
The generation of KPE license key is now limited only for KPP partner account. <br/>
Therefore, if you don't already have a working license key you cannot activate Adhell3. <br/>

# Disclaimer
Adhell3 is merely an app that is using the Samsung Knox SDK APIs. <br/>
In order to use these APIs, the Knox SDK and a KPE license key are needed. <br/>
These are Samsung's properties which are not available in this repository and therefore they need to be downloaded and obtained by the developer after accepting the agreement with by Samsung. <br/>
I am only making adhell3 available as a source code project. When a developer assembles/compiles an apk from this project, the developer is then responsible for how that apk and the proprietary material it contains will be used and distributed. I don't take any responsibilities for any damages caused by this app. <br/>

The Knox SDK can be downloaded here: https://partner.samsungknox.com/dashboard/download (only available for KPP partners) <br/>
The API can be found here: https://docs.samsungknox.com/devref/knox-sdk/reference/packages.html

Adhell3 is licensed under a Creative Commons Attribution-NonCommercial 4.0 International License.<br/>
You should have received a copy of the license along with this work. If not, see <http://creativecommons.org/licenses/by-nc/4.0/>.

## Features
- Mobile and Wi-Fi access disabler<br/>
Disable internet access when on mobile or/and Wi-Fi for specific apps. This can be useful to avoid watching videos accidentally by using mobile data.

- Custom firewall rule<br/>
This can be used for example to define a custom firewall rule to block ads for Chrome-based apps on port 53 for all IP addresses:<br/>
    `com.android.chrome|*|53` for Chrome, `com.sec.android.app.sbrowser|*|53` for Samsung Internet, etc.<br/>
Outside of DENY, it also supports ALLOW, REDIRECT and REDIRECT EXCEPTION rules.

- Whitelist a domain for a specific app<br/>
For when you want to block a domain systemwide, but you still need this domain for a particular app that won't work without it.<br/>
Instead of whitelisting the app, you can just whitelist that domain for this app.<br/>
Example: Block the domain `graph.facebook.com` systemwide, but allows it for Facebook Messenger so that it can be used for login:<br/>
    `com.facebook.orca|graph.facebook.com`

- Support local host source<br/>
A domains lists (provider) file can be located in internal, external storage on your device or picked from some cloud providers.<br/>

- Show the content of host source<br/>
Show the list of all potentially blocked domains from all active host sources combined.<br/>
This can be useful to check whether a particular domain is in the list.<br/>

- Backup and restore database<br/>
Backup the content of the database on internal storage so that later it can be used to restore the database if the database is somehow damaged.

- DNS per application basis<br/>
Set custom DNS for selected apps. Both IPv4 and IPv6 are supported. Only user apps are supported.

- Manage apps' components<br/>
Disable permissions, services, receivers, activities and content providers for for any app (System apps are not shown by default).

- Restrictions<br/>
Turn off google crash report, account sync or disallow camera and microphone! These are only few examples

- Proxy<br/>
Set global http proxy that will work for all apps (some might not work with it).

## Building APK
- Download the script from https://gitlab.com/fusionjack/adhell3-scripts
- Follow the instruction in the README

## Customization
### Change the default 15k domain limit
* Put `domain.limit` in `app.properties`, e.g. `domain.limit=50000`

### Make license key available when activating Adhell3
* Put `skl.key` in `app.properties`, e.g. `skl.key=XXXXXXXXXXXXXXXXXXXXXXXXXXXXX`
* If you use this option, beware that when you distribute your app, the keys will be in plain text
* This is just for convenience so that you don't need to transfer your keys to your phone manually, and is only useful on a new installation

### Domain prefix
* Prefix all domains (with the exception of Filter Lists) with * or nothing.
* Valid `domain.prefix` options: `domain.prefix=true`, `domain.prefix=false`
* If you choose not to define a prefixing option, domains will not be prefixed.
* Put `domain.prefix` in `app.properties`
* `domain.prefix=true` -> prefix all domains with `*`
* `domain.prefix=false` -> don't prefix anything, keep domains as they are
* The default is: nothing -> no prefix

### Hidden features
* Beware that enabling some hidden features may cause the device to malfunction if they are not used with precaution, especially when disabling system apps. Enable them at your own risk.
* Add `enable.disableApps=true` in `app.properties` -> to enable 'Disable Apps' feature: <br/>
An ability to disable user or system applications entirely
* Add `enable.appComponent=true` in `app.properties` -> to enable 'App Component' feature: <br/>
An ability to disable app's permissions, services and receivers. Only user apps are supported.
* Add `appComponent.showSystemApps=true` in `app.properties` -> to list system apps in 'App Component'

### Override default host with your own compiled host
* Put `default.host` in `app.properties`, e.g. `default.host=https://gitlab.com/fusionjack/hosts/raw/master/hosts`

### Change default log duration
* Put `blockedDomain.duration.ui` in `app.properties`, to change number of domains in last x hours that are shown in home tab log e.g. `blockedDomain.duration.ui=48`
* Put `blockedDomain.duration.db`, to change number of domains in last x hours that will be stored in database which can be dumped into text file e.g. `blockedDomain.duration.db=168`

## Credits
* Adhell3 is based on FiendFyre's Adhell2 which is heavily modified by me.<br/>
* Big thanks to @mmotti who provides a host file for Adhell3. You can visit his Github here: https://github.com/mmotti/mmotti-host-file
* Adhell3 is using icons from https://material.io/icons
