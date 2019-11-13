# Summary
When using replication technology between data centers, an item on many testing plans to validate data between the two datacenters. This projects is to server as an **example** of how one might begin to think about this. This project is to prove out concepts on how to implement a GemFire client applicaton to connect to two distributed systems and run validtion checks. It is important to note that the systems must be static with no Gateway queue size as WAN technology is _eventually consistent_. At the time of implementing this **example**, thoughts of performance improvements through **GemFire functions** was also considered, however, not implemented for this. 

## Build
```
mvn clean install -DskipTests
```

## Run GemFire start scripts locally

In two gfsh prompts execute scripts found in `db` directory.

```
    _________________________     __
   / _____/ ______/ ______/ /____/ /
  / /  __/ /___  /_____  / _____  / 
 / /__/ / ____/  _____/ / /    / /  
/______/_/      /______/_/    /_/    9.8.4

Monitor and Manage Pivotal GemFire
gfsh>run --file=start-a.gfsh
```

```
    _________________________     __
   / _____/ ______/ ______/ /____/ /
  / /  __/ /___  /_____  / _____  / 
 / /__/ / ____/  _____/ / /    / /  
/______/_/      /______/_/    /_/    9.8.4

Monitor and Manage Pivotal GemFire
gfsh>run --file=start-b.gfsh
```

## Run application 

### Properties File
To make the application easier to manage, a set of profiles have been set as well as a simple application.properties file.

Configure application.properties with GemFire `region` names that you wish to verify between clusters with `gemfire.wan.regions` property.
Configure the nessisary pool settings to connect to both distributed systems.

An example of this is included already in the repository: 

```
spring.data.gemfire.pool.site1.locators=localhost[10334],localhost[10335]
spring.data.gemfire.pool.site2.locators=localhost[10336],localhost[10337]
spring.data.gemfire.pool.default.locators=${spring.data.gemfire.pool.site2.locators},${spring\
  .data.gemfire.pool.site1.locators}

gemfire.wan.regions=cat,dog,customer
```

### Profiles
Three profiles are included in the application to exercise various functionality:
* `spring.profile.active=test`
   * This profile will execute the main validation code on regions provided.
* `spring.profile.active=loadSite1`and `spring.profile.active=loadSite1`
   * These are profiles I used when testing Domain Objects beyond String,String. 
   * This will use MOCK_DATA.json to seed GemFire with Customer objects to validate. 
   
### Validations
Currently the project handles three validations:
* regionEntrySize: The number of entries in the region.
* keyMatchers: A validation that keys in one distributed system match keys in the other.
* dataMatchers: A validation that actual compairs value for keys between distributed system. 

_**Note:**_ Addition validtions can be added in ValidationService following the example patterns provided to suit use cases. 
 
