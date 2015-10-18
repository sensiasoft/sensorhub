Creating a new Maven module
---

So that it can be more easily integrated to OpenSensorHub build process, we advise you to package each OSH module (or set of similar modules) as a separate eclipse project and corresponding Maven module. 

If you are using Eclipse with the M2E plugin as described in the [Developer's Guide](../dev-guide.html), you can create the new project by following the steps below:


### Create a new Maven Eclipse project

  * Right click in package explorer and select _"New > Other"_
  * Go down to the _"Maven"_ section and select _"Maven Project"_ then click _"Next"_
  * Check _"Create a simple project"_ and click _"Next"_
  * Set _GroupId="org.sensorhub"_ and _ArtifactId="sensorhub-{moduletype}-{modulename}"_ (for instance _sensorhub-driver-axis_ for an Axis camera driver)
  * Fill up the name and description fields with meaningful information
  * Set Parent Project to _GroupId="org.sensorhub"_, _ArtifactId="sensorhub-all"_, _Version=0.5_ and click _"Finish"_


### Move the project inside the sensorhub main folder

By default your project should have been created at the root of the workspace. To move it into the correct sub-folder, follow these steps:

  * Right click on the newly created project
  * Select _"Refactor > Move"_ from the popup menu
  * In the location field, insert "osh-xxx/" before the project name in order to obtain the following path: _"path/to/your/workspace/osh-xxx/sensorhub-{moduletype}-{modulename}"_ 


### Add dependency to SensorHub core software module

  * Open the _pom.xml_ that was created at the root of the project (It should open in a special editor if the M2E plugin was properly installed)
  * Go to the _"Dependencies"_ tab and click the _"Add"_ button
  * Enter _"sensorhub"_ in the search box (above the Search Results section)
  * Wait for results to appear, select _"sensorhub-core"_ in the list and click OK
  * You can also edit the pom.xml to override the organization and developer name that are inherited from the parent project by putting your own.
  * Save the file and close it 
  * Depending on the type of module you develop, you may have to add depencendies to other modules as well, and even to external libraries that you module may depend on.


### Enable Git version control

To associate the new project with the Git repository within Eclipse, follow these steps:

  * Right click on the project and select _"Team -> Share"_ from the context menu
  * Select _"Git"_ en click _"Next"_
  * Check _"Use or create repository in parent folder of project"_ so that your new project can be associated to the main sensorhub repository
  * Select you project in the list and click _"Finish"_
