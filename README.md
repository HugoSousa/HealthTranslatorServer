# HealthTranslatorServer

1) Clone the project to your machine. 
(The code can be compiled in your own machine and only the resultant .war file needs to be copied to the server machine. The following steps are to be done in the server machine)

2) Install MySQL.

3) Create the database and insert the data, using the dumps in folder /database. Respect the order in the english version. 

4) Install Glassfish.
The following tutorial helped me with the commands needed to install and deal with Glassfish:  
https://www.digitalocean.com/community/tutorials/how-to-install-glassfish-4-0-on-ubuntu-12-04-3

5) Setup a JDBC connection in Glassfish (follow the guide @ https://computingat40s.wordpress.com/how-to-setup-a-jdbc-connection-in-glassfish/).  
Pool name: connPool  
Resource type: java.sql.Driver  
Database Driver Vendor: MySQL    
Configure the database URL, username and password and keep the remaining fields as default. 

6) Compile the Java code in your own machine, copy the generated .war file to the server. 

7) Deploy the .war file in Glassfish (check the deploy section in the previous Glassfish link) and run it. It should be working now.
