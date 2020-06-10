
Maintained by
 [![Perforce](https://www.perforce.com/themes/custom/themekit/logo.svg)](https://www.perforce.com)

 [![Jenkins Version](https://img.shields.io/badge/Jenkins-2.164.3%2B-green.svg)](https://jenkins.io/download/) [![Helix QAC](https://img.shields.io/badge/Helix%20QAC-2.2.0%2B-green.svg)](https://www.perforce.com/products/helix-qac)

# Intellectual Property Update

The PRQA product has become Helix QAC and Programming Research has become a Perforce company. As such, the PRQA plugin (now called Helix-QAC Plugin) has changed cosmetically to reflect this. There may be a dedicated Helix-QAC plugin at a suitable GitHub location in the future. If one is created, it will be linked to from here.

# Introduction to Helix-QAC static analysis using the Helix QAC plugin

This plug-in enables the Helix-QAC static analysis tools to be integrated easily with Jenkins.

This plug-in is maintained by Perforce. It was originally developed and maintained by Praqma as far as release 1.2.2. Programming Research maintained it thereafter up to 3.2.
Version 3.3.0 (this release) is maintained by Perforce. All service and feature requests should be sent directly to Perforce.

Prior to version 3.3.0 it supported Freestyle projects only, but now supports Pipeline projects. Prior to version 3.3.0 it supported Jenkins versions up to 2.107.2. This version (3.3.0) supports Jenkins version 2.164.3 onward.

Version 3.0.1 supported PRQA Framework version 2.2.0 onward and up to Jenkins version 2.107.2.
Version 3.3.0 will support PRQA Framework version 2.2.0 onward and Helix-QAC Framework 2019.1 onward.

#### Pipeline support requires Jenkins version v2.164.3 or later.

# Summary Installation Instructions
* Install/use [latest stable](https://www.jenkins.io/download/) Jenkins version that is 2.164.3 or later
* Install the [Pipeline](https://plugins.jenkins.io/workflow-aggregator/) plugin for Jenkins and restart Jenkins after installation
* Download and install this plug-in

# Features

This plug-in allows source code to be analyzed with Helix-QAC, Perforce's static analysis tools. The plug-in performs the following key tasks in the post-build stage, or as a pipeline step automatically:

* Analyzes a Helix-QAC project – optionally including dataflow and cross module analysis.
* Generates a compliance report
* Compares the total number of messages in the project against a configurable threshold and sets the build status to ‘unstable’ if the threshold is exceeded. This can be used as a gateway to preventing subsequent tasks from running.
* Optionally uploads the analysis results to Dashboard, the Helix-QAC web-based Quality Management System
* This plug-in is able to analyze configured Helix-QAC projects.
* HIS Metrics Report option has been added in this release.

The projects are configured using Helix QAC Framework. They can be configured using Helix-QAC tools to support the old analysis interface.

The plug-in allows the automation of the creation and analysis of Helix-QAC enabled projects. This enables the analysis to be performed as a part of a continuous integration strategy.

The plug-in displays a graphical history depicting the number of messages, and overall compliance levels in the project.

#### Please refer to the old [PRQA Plugin Wiki](https://wiki.jenkins.io/display/JENKINS/PRQA+Plugin) for configuration guidance, being mindful that references to PRQA in screenshots will have changed to Helix-QAC.

# Known Issues
* Graphs on the Freestyle project summary page may display twice.
* The completion of a Pipeline build on the projects summary page will not refresh the entire page. The user should refresh the page on completion of the job run.
* Reports may be missing colours on pie charts and legend keys. This is due to Jenkins default security settings regarding JavaScript and CSS where both are disallowed. To remedy, please follow the instructions in the top answer to this [Stack Overflow](https://stackoverflow.com/questions/35783964/jenkins-html-publisher-plugin-no-css-is-displayed-when-report-is-viewed-in-j) question.