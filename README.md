# edu.brandeis.lapps:app-scaffolding 

**Still under development, and will be release as 1.0.0 when dependencies (LAPPS Java APIs and parent pom) are stable. **

This Java package provides a JAR artifact that can be used as a base scaffolding for a LAPPS Grid application. 
The artifact and its [parent pom](https://github.com/brandeis-llc/lapps-parent-pom) are distributed via [Brandeis nexus reporisory](http://morbius.cs-i.brandeis.edu:8081/), and versions of those two artifacts (JAR and parent POM) must be kept synchronized. 

A LAPPS Grid application must use LAPPS Interchange Format (LIF) for I/O, but can be a simple CLI application as well as a LAPPS web service (WAR artifact). 
