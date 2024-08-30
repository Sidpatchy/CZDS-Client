# CZDS-Client
[![Build Status](https://img.shields.io/teamcity/build/s/CzdsClient_Build?server=https%3A%2F%2Fci.sidpatchy.com&style=flat-square)](https://ci.sidpatchy.com/buildConfiguration/CzdsClient_Build?branch=%3Cdefault%3E&buildTypeTab=overview)
[![GitHub License](https://img.shields.io/github/license/Sidpatchy/CZDS-Client?style=flat-square)](https://github.com/Sidpatchy/CZDS-Client/blob/main/LICENSE)
[![GitHub last commit](https://img.shields.io/github/last-commit/Sidpatchy/CZDS-Client?style=flat-square)](https://github.com/Sidpatchy/CZDS-Client/commits/main/)
[![JitPack](https://img.shields.io/jitpack/version/com.github.Sidpatchy/CZDS-Client?style=flat-square)](https://jitpack.io/#Sidpatchy/CZDS-Client)

A modern tool and Java library for downloading zone files from ICANN's CZDS API.

**CZDS-Client is in NO WAY associated with ICANN. It is a project built and maintained by Sidpatchy.**

## CLI
CZDS-Client includes a basic CLI. Usage details can be viewed below.

### Usage
```bash
usage: ZoneFile-Tools
 -a,--all              Download all zones
 -d,--debug            Enable debug mode
 -f,--path <arg>       Specify which directory zone files should be downloaded to -- defaults to './Downloads/'
 -h,--help             Show help
 -p,--password <arg>   ICANN CZDS Password
 -s,--show-approved    Lists all TLDs you are approved to access
 -u,--username <arg>   ICANN CZDS Username
 -v,--version          Show version
 -z,--zone <arg>       Specify a zone file to download.
```

### Examples

```bash
# Download the .com zone file:
java -jar CZDS-Client-1.0.jar --username 'email@example.com' --password 'password123' --zone 'com'

# Download all zone files you're authorized to download:
java -jar CZDS-Client-1.0.jar --username 'email@example.com' --password 'password123' --all

# List which zone files you are authorized to download:
java -jar CZDS-Client-1.0.jar --username 'email@example.com' --password 'password123' --show-approved
```

## Library
CZDS-Client is built using `CompletableFuture`s for non-blocking API calls, enabling efficient handling of concurrent operations.
Further, it is built using standard Java classes to minimize the learning curve.

### Usage
**Download the '.com' zone file:**
```java
String username = "email@example.com";
String password = "password123";

CZDSClient client = new CZDSClient(username, password);
client.getDownloader().downloadZoneFile("com").join();
```

**Download all authorized zone files:**
```java
String username = "email@example.com";
String password = "password123";

CZDSClient client = new CZDSClient(username, password);
client.getDownloader().downloadAllApprovedZoneFiles().join();
```
