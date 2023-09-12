from json import dumps
from subprocess import Popen
from boto3 import client
from time import time, sleep
import sys

TABLE_NAME = 'cse546_student_data'
ACCESS_KEY_ID = 'AKIASQEPCWXYL3EB57T6'
SECRET_KEY = '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'
VIDEO_UPLOAD_BUCKET = 'cse546-project2-video-uploads-abaner40'
LAMBDA_NAME = 'cse546-projecct3-lambda'
REGION = 'us-east-1'

def poll_s3_video_upload_bucket():
	s3_client = client('s3', aws_access_key_id=ACCESS_KEY_ID, aws_secret_access_key=SECRET_KEY)
	lambda_client = client('lambda', aws_access_key_id=ACCESS_KEY_ID, aws_secret_access_key=SECRET_KEY, region_name=REGION)

	while True:
		print('Fetching objects from s3')
		resp = s3_client.list_objects(Bucket=VIDEO_UPLOAD_BUCKET)

		try:
			for obj in resp['Contents']:
				# Get the key of the object in s3
				print(obj['Key'])
				key = obj['Key']

				# Invoke lambda with the key in the payload
				payload = dumps({'key': key})
				print('Triggering lambda with payload: ', payload)
				response = lambda_client.invoke(FunctionName=LAMBDA_NAME,InvocationType='RequestResponse',Payload=payload)
				# print('Response from lambda: ', response)

				# Delete the key from the s3 bucket so that it is not processed twice
				s3_client.delete_object(Bucket=VIDEO_UPLOAD_BUCKET, Key=key)

		except Exception as ex:
			print(ex)
			sleep(10)

		# sleep(0.2)

if __name__ == '__main__':
	poll_s3_video_upload_bucket()

	# logfile = f'trigger-{time()}.log'
	# Popen(['touch', logfile]).wait()

	# with open(f'./{logfile}', 'w') as f:
	# 	print(f'Check log file: {logfile}')
		# sys.stdout = f
		# poll_s3_video_upload_bucket()
