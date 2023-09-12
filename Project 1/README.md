### Cloud Computing Project 1 - IaaS
1. The Project aims at developing a cloud app that can recognise images using deep learning model and cloud resources.
2. The deep learning model is provided as an AWS image (ID: ami-01e547694fca32b28, Name: app-tier, Region: us-east-1)

###### Members and Contribution:
Shubham Chawla: 
Shubham started with the development of the App Tier application of the project. This part entailed writing a Springboot application that listens to an SQS queue for image keys. Once received, the application downloads the image from the S3 bucket and executes an image classification script on that image. The python script's results are extracted and uploaded to another S3 bucket, and the final acknowledgment is sent to Web Tier using an S3 response queue. Here, platform-dependent paths, such as script name, script path, and download directory, are picked via Spring's run arguments for environment-focused development.

Anurag Banerjee:
Anurag was responsible for developing the Web Tier of the project, which receives requests from the workload generator, sends the message to an SQS request queue, and uploads the image to an S3 bucket. The Springboot application then waits for an acknowledgment from the App-Tier on a dedicated SQS response queue and displays the result on the console of the Web-Tier instance. AWS-related properties are passed via run arguments, ensuring environment-focused development and security.  

Vedant Munjaji Pople:
Vedant focused on Web and App Tier's functioning on AWS. This responsibility primarily entailed ensuring the load balancing of App Tier's instances. While working on CloudWatch and capturing the metrics of the Auto Scaling Group, App Tier's instance count was the function of the number of visible messages on the SQS request queue. Vedant maintained the project documentation, ensuring milestone resolutions.


###### Credentials and Links:

- SQS queue 1: Request Queue: https://sqs.us-east-1.amazonaws.com/172098762224/cse546-project1-request-queue
- SQS queue 2: Response Queue: https://sqs.us-east-1.amazonaws.com/172098762224/cse546-project1-response-queue
- S3 bucket 1: input-bucket: image-uploads-abaner40
- S3 bucket 2: output-bucket: image-classification-abaner40
