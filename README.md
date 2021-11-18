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
UIN:

Tru Nguyen  
UIN: 654073093  
tnguy276@uic.edu  

## Project
This repository is organized into X different subprojects.

- LogFileGenerator

The following sections describe the functionalities implemented in all of them.

# LogFileGenerator

The first project, logGenerator, provides an extension of the log generator coded by Professor Mark.

We have design the overall architecture following this schema

![alt text](docs/architecture.png)

All the instances of the logGenerator will pack their own data into a single log file. 

All the output log files of the instances will be store into a common folder <code>output</code> following the naming convention of output1.log for instance 1, output2.log for instance 2 ...  and so on.

For example, if we instanciate 3 logGenerators, our output directory tree will look like this

- output
	- output1.log
	- output2.log
	- output3.log


In order to run multiple instances of the logGenerator we can launch the script under the <code>jar</code> folder named <code>launch.sh</code>.


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

```
chmod +x launch.sh
```
Please make sure that the script and the jar <code>logGenerator.jar</code> are in the same directory.

Now, we can run the script

```
./launch.sh
```

The script will ask us how many instances of the logGenerator we would like to run. Once we provide the number, it will automatically instanciate them for us.





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
