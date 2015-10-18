Developer's Guide
---

This guide is meant to help you setup a development environment based on the Eclipse IDE so that you can extend OpenSensorHub (OSH for short) with your own sensor drivers, web services and other components.

Don't forget to send us a Pull Request if you want to contribute your work back to this project. Other users may be interested by your modules and bug fixes! 

Of course, contributing new modules to the community is optional as our license does not prevent proprietary and commercial derived work. However, keep in mind that **if you modify the source files we provide, you must make it available publicly in source form**. 

This page provides instructions for three possible options, depending on your level of involvement:

  * Exploring the code online
  * Downloading and building from source with Maven or Eclipse
  * Contributing software and fixes to the project



### Exploring the Code

If you just want to explore the code, you can browse the source online directly on [Github](https://github.com/opensensorhub). Alternatively, you can download it to your computer using the *Download ZIP* link on each GitHub repository or using the `git` program (please see the next section if you want to do just that).

To start with, the repositories of interest are [OpenSensorHub Core](https://github.com/opensensorhub/osh-core) and [Sensor Drivers](https://github.com/opensensorhub/osh-sensors).



### Downloading and Building from Source

Below are the steps to download and build the code using either command line tools or the Eclipse IDE.

#### Using Command-Line Tools

If you want to build the code and run it on your computer, you'll need `git` and `Maven 3`.
To clone the code repository locally, first create a directory called `opensensorhub` (or anything else you want really) and `cd` in this directory:

     > mkdir opensensorhub
     > cd opensensorhub

Then and use the following commands to clone the two main repositories:

     > git clone --recursive https://github.com/opensensorhub/osh-core
     > git clone https://github.com/opensensorhub/osh-sensors

This will create folders containing the code of the different Maven modules. You can also clone other repositories of the project to get other types of modules. The `services` and `security` repositories are probably of interest if you want to go further. There are also some Android specific modules in the `android` repository if you are interested in deploying on Android.

You can then build the project and install it to your local Maven repository by launching the following commands:

First build the core modules:

     > cd core
     > mvn clean install 

Then build the sensor drivers you are interested in. To start with, you can build the simulated sensor since they don't require you to connect any hardware.

     > cd ../osh-sensors/sensorhub-driver-fakegps
     > mvn clean install
     > cd ../sensorhub-driver-fakeweather
     > mvn clean install

_Note 1: The first time you launch Maven, the build process can take a while because Maven goes to fetch its own dependencies (i.e. Maven plugins) as well as OpenSensorHub's dependencies. Later builds will go faster because these dependencies are cached in a local Maven repository._

_Note 2: Some of the JUnit tests automatically run during the 'test' phase of the OSH build process need to instantiate a server on port 8888. These tests will fail if something else is running on this port when you launch the commands above._

You can then run OpenSensorHub with an example configuration file. For instance, the following command launches OpenSensorHub configured with some simulated sensors, storage databases and an SOS service:

     > cd ../../osh-core/sensorhub-test
     > mvn -N -e exec:java -Dexec.mainClass="org.sensorhub.impl.SensorHub" -Dexec.args="sensorhub-test/src/test/resources/config_fakesensors_with_storage.json db"


#### Using Eclipse

We provide Eclipse project configuration directly from the repository so it is the easiest way to get started, especially if you're already familiar with Eclipse. 

##### Pre-requisites

Make sure you have the following Eclipse components installed:
  
  * Eclipse Helios or newer (the exact steps described here are for Luna)
  * Egit plugin for Eclipse (included in "Eclipse IDE for Java Developers" release)
  * Maven plugin for Eclipse M2E (you can install it using the "Help > Install New Software..". You will find it under the "Collaboration" section of the Eclipse releases repository) 

##### Clone the project in your Eclipse workspace

  * In the Package Explorer, right click and select "Import" from the popup menu
  * Open the "Git" category, select "Projects from Git" and click "Next"
  * Select "Clone URI" and click "Next"
  * In the "URI" text box, enter the URL of the OpenSensorHub Core repository `https://github.com/opensensorhub/osh-core.git` and click "Next"
  * Leave "master" selected and click "Next"
  * You can leave the Directory settings as-is on this page or change it to the location of your choice (Note that the Egit manual discourages cloning directly in the Eclipse workspace for performance reasons, however we haven't had any issue doing this with the opensensorhub code base. If you want to do like us, change the "Directory" to points directly to a sub-directory of your Eclipse workspace, for instance, "/home/user/workspace/osh-core")
  * Select "Clone submodules" and click "Next"
  * After the download is complete, leave "Import existing projects" selected and click "Next"
  * Leave all projects selected and click "Finish"
  * All projects should be imported successfully and visible in the "Package Explorer". Everything should compile without error.
  * If you like to keep your workspace tidy, you can group all the projects we just imported in a single Working Set

Repeat the steps above with the desired repositories of the opensensorhub github account. You'll need at least some sensor drivers to test the software. You can get these from the following repository:

  `https://github.com/opensensorhub/osh-sensors.git`

There may be other repositories of interest for you:

  * Android: `https://github.com/opensensorhub/osh-android.git`
  * Other Services : `https://github.com/opensensorhub/osh-services.git`
  * Data-Processing : `https://github.com/opensensorhub/osh-processing.git`
  * Security Stuff : `https://github.com/opensensorhub/osh-security.git`
  

### Contributing Code to the Project

If you want to contribute, we feel the best way is that you create your own fork on GitHub, work on it, and when you have something working and tested, send us a Pull Request. To set this up, please follow the steps below:

#### Fork one or more repository of the project

The first step is to fork a repo by clicking the [Fork](http://help.github.com/articles/fork-a-repo/) button on GitHub. This will clone the original code to your own GitHub account so you can then modify it and/or add to it as you wish. For this you'll need to have a GitHub account (it can be done in 30s using your email address) and log into it.

Forking the project this way will allow you to send us [Pull Requests](http://help.github.com/articles/using-pull-requests/) via GitHub which makes it much easier for us to incorporate your contribution to the master branch. In addition, it creates a community around the software and lets others see what contributors are up to even before a patch is submitted. This can help you get the proper guidance when necessary.

#### Clone your GitHub repository

Clone your new GitHub repository locally by following the steps in the [Download and Build](#Option_2_Downloading_and_Building_from_Source) section except you'll be using your own fork URL (e.g. https://github.com/yourusername/osh-***) instead of the *opensensorhub* version.

#### Work on something new!

You can then start modifying the code and/or add new modules/features. We don't have coding guidelines yet but try to mimic the code that is already there. Don't forget to include Javadoc, especially on public parts of your APIs, and also inline comments explaining the different steps of your code.

Whether you're trying to fix bugs or adding a brand new functionality, don't hesitate to tell us early-on what you're planning to work on. We may be able to point you in the right direction or maybe to somebody who has similar needs than you.

You can start by reading the instructions to [Create a New Module](sensorhub-core/adding-new-modules.html) and [Add a New Sensor Driver](sensorhub-core/your-first-sensor.html) for instance.

Also see the [Eclipse Tips](#Eclipse_Tips) section if you encounter problems while creating a new module.

#### Get the latest updates from us

While you're working on your stuff, don't forget to pull changes from the main repository once in a while. This will greatly help us merge your changes into the main branch when we receive your Pull Request. You can either do that from command line git or within Eclipse:

##### Using the `git` command

First add a new remote pointing to the *opensensorhub* master branch (you only have to do that the first time). For example, for the osh-core repository:

     > git remote add upstream https://github.com/opensensorhub/osh-core

Then pull changes from the "upstream" remote:

     > git pull upstream master
     > git submodule update

_Note 1: The `submodule update` command is only required in the `osh-core` repo that has submodules._

_Note 2: that you may have to manually merge things with your working copy if you have made conflicting changes._

##### Using Eclipse

First add a new remote pointing to the *opensensorhub* master branch (you only have to do that the first time):

  * Open the "Git Repositories" view (Window -> Show view -> Other -> Git)
  * Open the "sensorhub" repository, right click on "Remotes" and select "Create Remote"
  * Enter "upstream" as the remote name, select "Configure fetch" and click "OK"
  * Click the "Change" button next to the URI text box
  * Enter `https://github.com/opensensorhub/osh-core` as the URI and click "Finish"
  * Click "Save"

Then pull changes from the "upstream" remote:

  * Open the "Git Repositories" view (Window -> Show view -> Other -> Git)
  * Right click on the "osh-core" repository and select "Remote -> Fetch" in the popup menu
  * Select the "opensensorhub" remote in the "Configured remote repository" item and click "Finish"
  * Right click on the "Submodules" folder and select "Update Submodule" from the popup menu
  
You'll then eventually have to merge our changes with yours using the Egit merge command. Please see [Git Documentation](https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging#Basic-Merge-Conflicts) for more details

Note: Also don't forget to import new Eclipse projects that may have been added since your last update. For this, follow these steps:

  * Right click in your workspace and select "Import..." in the context menu
  * Select "Existing project into workspace" from the "General" section and click "Next"
  * Browse to the folder where you cloned our repo (usually called "osh-core" for the core software)
  * Select the missing projects in the list (all the projects that are not already in your workspace should already be selected) and click "Finish"   

#### Push your changes to your own repo

You can push your changes to your own GitHub repo at any time, even if your code doesn't work yet. Remember this is your own sandbox so you won't mess up anybody else code base. We actually recommend that you do that often since it will provide you a good backup of your work, with full history.

You won't be able to push directly to the opensensorhub repos directly since you don't have write permissions (not until you become part of the team anyway). 

##### Using the `git` command

To do this with git command line tool, first stage and commit your changes locally:

     > git commit -am "Your commit message"

and then push them to your remote GitHub repository:

     > git push

(Please see the [git online documentation](http://git-scm.com/book/en/v2) for more details and other ways to use git)

##### Using Eclipse

Within Eclipse, follow these steps:

To commit your changes locally:

  * Right click on one of the Eclipse project with a name starting with "sensorhub"
  * Select "Team -> Commit" from the popup menu
  * Enter a commit message and select files you want to commit
  * Click "Commit" (or "Commit and Push" if you want to commit locally and push to your remote repository in a single step)
  * If you have just pressed "Commit" you will see a arrow with a number on the right of the project names in the package explorer. This indicates that you have N local changes that need to be pushed to the remote repository (i.e. in git terms, your local repository is N commits ahead of your remote).

If you only want to push your last committed changes to your remote repository:

  * Right click on one of the Eclipse project with a name starting with "sensorhub"
  * Select "Team -> Push to Upstream" from the popup menu
  * Click OK

(Please see [Egit online documentation](http://wiki.eclipse.org/EGit/User_Guide) for more advanced functionality)

#### Contribute your code

When you feel you're ready to contribute all or some of your changes to the community, please send us a [Pull Request](http://help.github.com/articles/using-pull-requests/) via GitHub.

So that we can better evaluate your contribution, please describe your improvements in as much details as you can. We'll do our best to process *Pull Request* as fast as possible.

**Thanks in advance for your contribution!**



### Eclipse Tips

##### Update Maven Settings

One problem we have encountered several times with Eclipse is that the POM files and Projects Settings get out of sync and it causes various Java and/or Maven related dependency errors (e.g. dependency YYY cannot be found, etc). If you get such errors even though everything seems fine in your POM and code, you may have to follow these steps to resync eveything:

  * Click one of the SensorHub module project
  * Select "Maven > Update Project..." from the context menu
  * Click the "Select All" button
  * Confirm by clicking "OK" 