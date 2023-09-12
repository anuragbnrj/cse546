from subprocess import Popen
from boto3 import client
from time import time, sleep
import sys
import csv

TABLE_NAME = 'cse546_student_data'
ACCESS_KEY_ID = 
SECRET_KEY = 
CLASSIFICATION_RESULT_BUCKET = 'cse546-project2-classification-outputs-abaner40'
REGION = 'us-east-1'

def poll_s3_video_upload_bucket():
	s3_client = client('s3', aws_access_key_id=ACCESS_KEY_ID, aws_secret_access_key=SECRET_KEY)
	while True:
		print('Fetching objects from s3')
		resp = s3_client.list_objects_v2(Bucket=CLASSIFICATION_RESULT_BUCKET)

		try:
			for obj in resp['Contents']:
				# Get the key of the object in s3
				# print(obj)
				key = obj['Key']
				full_obj = s3_client.get_object(Bucket=CLASSIFICATION_RESULT_BUCKET, Key=key)
				data = full_obj['Body'].read().decode('utf-8').splitlines()
				records = csv.reader(data)
				res = []
				for eachRecord in records:
					res = res + eachRecord

				res = ",".join(res)
				print("Key:", key ,"Result:",res)

				# Delete the key from the s3 bucket so that it is not processed twice
				s3_client.delete_object(Bucket=CLASSIFICATION_RESULT_BUCKET, Key=key)

		except Exception as ex:
			print("Exception occured! Bucket might be empty!",ex)
			sleep(10)

		# sleep(0.2)

if __name__ == '__main__':
	poll_s3_video_upload_bucket()

	# logfile = f'output-{time()}.log'
	# Popen(['touch', logfile]).wait()

	# with open(f'./{logfile}', 'w') as f:
	# 	print(f'Check log file: {logfile}')
	# 	sys.stdout = f
	# 	poll_s3_video_upload_bucket()
