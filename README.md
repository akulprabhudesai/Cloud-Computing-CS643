# Cloud-Computing-CS643
Perform Image and text recognition using Amazon Recognition serivicw of AWS and Java with Infrastructure as a Service platform
1) create two ec2 Instances on aws using Infrastrucuture as a Service platform.
2) Create jar files of ImageRecognition.java and Textrecognition.java files by using IDE as follows:
 i) For ImageRecognition.java
   a. In IDE, go to file --> Export --> Java --> Runnable JAR file.
   b.Launch configuration : ImageRecognition and Export destination : (suitable location)
 ii) For TextRecognition.java
   a. In IDE, go to file --> Export --> Java --> Runnable JAR file.
   b.Launch configuration : TextRecognition and Export destination : (suitable location)
3) Install JDK in both of them by entering following two commands:
	1. -wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u141-b15/336fa29ff2bb4ef291e347e091f7f4a7/jdk-8u141-linux-x64.rpm
	
	2. -sudo yum install -y jdk-8u141-linux-x64.rpm
	
4) Upload credentials text file on both the instances with credentials found on aws account details --> AWS CLI --> show. This you have to do periodically since credentials are valid for some finite amount of time. You can copy and paste new credentials on existing file using nano credentials.
For Uploading credentials first time on EC2 instances use following command;
    scp -v -i .\<name of key>.pem .\<credentials file name> <EC2 public dns>:/home/ec2-user/
5) Upload respective jar files on EC2 instances by command: 
 scp -v -i .\<Name of key>.pem .\<Name of Jar file>.jar <EC2 public DNS>:/home/ec2-user/
   a) ImageRecognition.jar file --> Instance1
   b) TextRecognition.jar file  --> Instance2

6) Run TextRecognition.jar on Instance2 by entering java -jar TextDetection.jar
7) Now run ImageRecognition.jar on Instance1 by entering command java -jar ImageRecognition.jar
8) sometimes sqs messages get lost before reaching sqs queue. In such case, run the ImageRecognition.jar again on Instance1 till you start getting output on Instance2.
9) For verification, I am printing image index name which is send to sqs on ImageRecognition instance and image index with detected label (if any) on textRecognition instance.
10) After TextRecognition functioning is compeye, result.text file will be created in the Textrecognition instance which contains the indexes of images which contains car and text(if present). 
11) To see the result.text file, type ls on ec2 Instance2, it will give list of files present. There you will find result.text file. type cat result.text to see content of the file.
