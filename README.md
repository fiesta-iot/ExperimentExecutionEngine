# Experiment Execution Engine (EEE)

### To install EEE

Before proceeding with the installation make sure:
- mysql is installed and running
 - To start you can use `sudo <PATH_TO_MYSQL.SERVER>/mysql.server start`. This is sometime `/usr/local/mysql/support-files/`
- ERM component is installed and running, and `utils.fiesta-iot` library is accessible to maven
- Authentication and Authorization component (OpenAm) is installed and running.
- Wildfly is installed and running. The code has been tested to successfully work on WildFly v10.0.0


Then 
- set USERNAME and PASSWORD in PATH_TO_SCHEDULER_PARENT/scheduler-parent/schedulerServices/src/main/quartz-schema.sql
- execute 
 `mysql -u root -p < PATH_TO_SCHEDULER_PARENT/scheduler-parent/schedulerServices/src/main/quartz-schema.sql
- copy following properties to `fiesta-iot.properties` after changing the `<HOST>`, `<OPEN AM ADMIN USER>`, `<OPEN AM ADMIN USER'S PASSWORD>`. Following assumes that all the components are deployed in one machine and have a same host. In case you have different HOST for ERM, EEE, EMC, Data repository, and Security server, use appropriate HOST. `fiesta-iot.properties` could be found in PATH_TO_WILDFLY/wildfly-10.0.0.Final/standalone/configuration. In case it is not available create `fiesta-iot.properties`.

```
eee.scheduler.ERMSERVICES=<HOST>/experiment.erm/rest/experimentservices
eee.scheduler.METACLOUD=<HOST>/iot-service/queries/execute/global
eee.scheduler.FILEPATHS=<PATH_TO_FILES_WHERE_YOU_WANT_TO_STORE>
eee.scheduler.GETEXPERIMENTSERVICEMODELOBJECT=/getExperimentServiceModelObject
eee.scheduler.GETEXPERIMENTMODELOBJECT=/getExperimentModelObject
eee.scheduler.SECURITYGETUSER=<HOST>/openam/json/users?_action=idFromSession
eee.scheduler.LOGIN=<HOST>/openam/json/authenticate
eee.scheduler.OPENAMUSERNAME=<OPEN AM ADMIN USER>
eee.scheduler.OPENAMPASSWORD=<OPEN AM ADMIN USER'S PASSWORD>
```
- configure `<PATH_TO_SCHEDULER_PARENT>/scheduler-parent/schedulerPersistance/src/main/resources/hibernate.cfg.xml` to reflect the username and password.
- configure `<PATH_TO_SCHEDULER_PARENT>/scheduler-parent/schedulerServices/src/main/resources/quartz.properties` to reflect the username and password

Once the above steps are done use following
``` sh
cd <PATH_TO_SCHEDULER_PARENT>/scheduler-parent
mvn clean install
cd <PATH_TO_WILDFLY_DEPLOYMENT_FOLDER>/deployments/
rm -rf scheduler*
cp <PATH_TO_SCHEDULER_PARENT>/scheduler-parent/schedulerServices/target/schedulerServices.war <PATH_TO_WILDFLY_DEPLOYMENT_FOLDER>/deployments/
```

Once reployed its APIs can be accessed using `<HOST>/schedulerService/scheduler/<API>`, `<HOST>/schedulerService/monitoring/<API>`, `<HOST>/schedulerService/polling/<API>`, and `<HOST>/schedulerService/subscription/<API>`. 

The EEE is supported by [Experiment Management Console](https://github.com/fiesta-iot/ExperimentManagementConsole). Please refer to its README for installation and other details.

The EEE API documentation is provided. We provide 2 versions which you can use. One contains list of APIS that can be used by Adminstrator while other only by the users. These files are called `docs/apiAdmin.json` and `docs/apiUser.json` respectively.

### To install API Docs 

Before proceeding with the configuration make sure you have Swagger-UI. Do the following:
- in `api.json` file change `<HOST>` accordingly
- in `index.html` file change the url in the line 36 (`localhost`) to reflect the `<HOST>`
- in `swagger-ui.js` file find 

  ``` javascript
  showPetStore: function(){
    this.trigger('update-swagger-ui', {
      url:'HOST/api.json'
    });
  },
  ```
  and change the `url` value to reflect the `<HOST>`.

- copy the folder `Swagger-UI` to `<WILDFLY>/standalone/deployment/`. If you want you can use any other name, EEEapidocs as an example.

- change `standalone.xml` with following (notice that you just need to add lines with `/EEEapidocs/` incase you have other name change accordingly). The following 2 configurations are for 2 versions of wildfly. The first configuration is for wildlfy 10.1.0.

``` xml
<server name="default-server">
    <http-listener name="default" socket-binding="http" redirect-socket="https" enable-http2="true"/>
    <https-listener name="https" socket-binding="https" security-realm="ApplicationRealm" enable-http2="true"/>
    <host name="default-host" alias="localhost">
        <location name="/" handler="welcome-content"/>
        <location name="/EEEapidocs/" handler="EEEapidocs"/>
        <filter-ref name="server-header"/>
        <filter-ref name="x-powered-by-header"/>
    </host>
</server>
```

This following configuration is for wildlfy 10.0.0.
``` xml
<server name="default-server">
    <http-listener name="default" socket-binding="http" redirect-socket="https"/>
    <host name="default-host" alias="localhost">
        <location name="/" handler="welcome-content"/>
        <location name="/EEEapidocs/" handler="EEEapidocs"/>
        <filter-ref name="server-header"/>
        <filter-ref name="x-powered-by-header"/>
    </host>
</server>
```

- finally replicate  `<file name="EEEapidocs" path="${jboss.home.dir}/standalone/deployments/EEEapidocs" directory-listing="true"/>` in 

``` xml
<handlers>
    <file name="welcome-content" path="${jboss.home.dir}/welcome-content"/>
    <file name="EEEapidocs" path="${jboss.home.dir}/standalone/deployments/EEEapidocs" directory-listing="true"/>
</handlers>
```

#### To Access

Use `http://<HOST>:<PORT>/EEEapidocs/`

### Authors

This work is (c) by Rachit Agarwal, Inria

### Licence

This component is licensed under a GPL V3.

### Acknowledgement

This work is funded by the European project\Federated Interoperable Semantic IoT/cloud Testbeds and Applications (FIESTA-IoT) from the European Union’s Horizon 2020 Programme with the Grant Agreement No. CNECT-ICT-643943. The authors would also like to thank the FIESTA-IoT consortium for the fruitful discussions.
