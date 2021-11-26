# Course Project
### The goal of this course project is to gain experience with creating a streaming data pipeline with cloud computing technologies by designing and implementing an actor-model service using [Akka](https://akka.io/) that ingests logfile generated data in real time and delivers it via an event-based service called [Kafka](https://kafka.apache.org/) to [Spark](https://spark.apache.org/) for further processing. This is a group project with each group consisting of one to six students. No student can participate in more than one group.
### Grade: 20%

## Authors
Andrea Cappelletti  
UIN: 674197701   
acappe2@uic.edu

Ajay Sagar  
UIN: 659867916  
anandi6@uic.edu

Cosimo Sguanci  
UIN: 670778611  
csguan2@uic.edu

Tru Nguyen  
UIN: 654073093  
tnguy276@uic.edu  

## Project
This repository is organized into X different subprojects.

- LogFileGenerator
- MonitoringService

The following sections describe the functionalities implemented in all of them.

## LogFileGenerator

The first project, logGenerator, provides an extension of the log generator coded by Professor Mark.

We have design the overall architecture following this schema

![alt text](docs/architecture.png)

All the instances of the logGenerator will pack their own data, that are generated over multiple days, into a single log file. 

All the output log files of the instances will be store into a common folder <code>output</code>following the naming convention of output1.log for instance 1, output2.log for instance 2 ...  and so on.

For example, if we instanciate 3 logGenerators, our output directory tree will look like this

- output
	- output1.log
	- output2.log
	- output3.log


To run the logGenerator on a EC2 instance follow the steps described below.

First thing first, log into your AWS console and start a Linux EC2 instance.

In order to do that, select launch instance and select

<code>Amazon Linux 2 AMI (HVM), SSD Volume Type - ami-03ab7423a204da002 (64-bit x86) / ami-0fb4cfafeead46a44 (64-bit Arm)</code>

Select <code>64-bit (x86)</code> and then <code>t2.micro</code>.

Make sure that SSH is enabled under security groups <code>SSH TCP 22 0.0.0.0/0 </code> and add your keypair when asked.

Now you should be able to login into your EC2 instance via SSH

In order to do so, run the command

```shell
ssh -i "linux.pem" ec2-user@ec2-54-241-68-63.us-west-1.compute.amazonaws.com
```

Where <code>linux.pem</code> is the name of your key and

<code>ec2-user@ec2-54-241-68-63.us-west-1.compute.amazonaws.com</code>

is the address of your instance.


Once you log in into your instance, in order to run the logGenerator you have to install
- Java SDK 8
- Scala
- SBT



### Install Java

To install Java, run the following command

```shell
sudo yum install java-1.8.0-openjdk
```
You may encounter the following error while running yum
```shell
File contains no section headers. file: file:///etc/yum.repos.d/bintray-sbt-rpm.repo
```
If you encounter that error, run
Solution
```shell
rm /etc/yum.repos.d/bintray-sbt-rpm.repo
```
In order to solve it, the go ahed and install Scala and SBT

### Install Scala

```shell
wget http://downloads.lightbend.com/scala/2.11.8/scala-2.11.8.rpm
sudo yum install scala-2.11.8.rpm
```
### Install sbt
```shell
curl -L https://www.scala-sbt.org/sbt-rpm.repo > sbt-rpm.repo
sudo mv sbt-rpm.repo /etc/yum.repos.d/
sudo yum install sbt
```

Now that we installed all the requirements to run the logGenerator, we are ready to run multiple instances of the logGenerator on our EC2 instance we can launch the script under the <code>jar</code> folder named <code>launch.sh</code>.


```bash
#!/bin/bash

echo 'Welcome! Please insert the number of instance I have to generate'

read instances

echo 'Okay, I will run {$instances} instances'

for i in $(seq 1 $instances);
do
    output_dir="output${i}.log"
    echo 'Running instance number' $i
    nohup java -jar logGenerator.jar $output_dir &
done

```

To run it, first we have to give it the right permission

```bash
chmod +x launch.sh
```
Please make sure that the script and the jar <code>logGenerator.jar</code> are in the same directory.

Now, we can run the script

```bash
./launch.sh
```

The script will ask us how many instances of the logGenerator we would like to run. 
Once we provide the number, it will automatically instanciate them for us in background.

If you want to stop the instances running you have to identify their PID (process identifier). In order to do that run 

```bash
lsof logGenerator.jar
```

Once you have identified the PIDs, you can stop the process running

```bash
kill PID
```

Where PID is the process identifier of the logGenerator instance.

### YouTube Video

A video explanation is available at this url: 


## MonitoringService

The `MonitoringService` is composed by the Akka actors that are constantly looking for changes in the log files using the Java NIO technology.

Based on a parameter that can be configured in the `application.conf` file, the idea is to instantiate one actor for each Log Generator, thus having one Akka actor for each log file that has to be monitored. The parameters that have to be configured are the following:

```
monitoringService {
    numOfLogGeneratorInstances = 2 # Should be equal to the number of deployed Log Generator instances
    basePath = "" # Contains the ABSOLUTE base path corresponding to the directory containing log files
    timeWindow = {
        start = "11:44:27"
        end = "11:44:27.999"
    },
    redisKeyLastTimeStamp = "LAST_TIMESTAMP"
    lineSeparator = " "
}
```

As we can see, the base path corresponding to the directory that Akka actors are tracking for changes. In addition, it is necessary to configure the time window that we want to consider when we are searching in log files in response to changes.

Every time there a log file is updated and the corresponding Akka actor reacts to it, the last timestamp that has already been passed to the Kafka component for the specific log file is stored in a local Redis instance. 
This allows us to stop and restart the `MonitoringService` without notifying again the Kafka component about logs that were already been streamed before. 
This means that the Redis Instance will contain N keys `LAST_TIMESTAMP-output1.log, LAST_TIMESTAMP-output2.log, ...`, with N that is the number of log files that are being monitored.

## Programming technology
All the simulations has been written in Scala using a Functional Programming approach.

While writing the simulations the following best practices has been adopted

- Large usage of logging to understand the system status;


- Configuration libraries and files to provide input values for the simulations;


- No while or for loop is present, instead recursive functions are largely used into the project.


- Modular architecture (code is divided by module to enhance maintainability)

## References
In order to produce the result obtained the following documents and paper
have been consulted.
