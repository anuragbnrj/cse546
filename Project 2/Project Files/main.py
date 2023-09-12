# This is a sample Python script.
import os
import tempfile
import cv2
import numpy as np
import face_recognition
import boto3
import shutil
from boto3.session import Session


TABLE_NAME = "cse546_student_data"
ACCESS_KEY = "AKIASQEPCWXYL3EB57T6"
SECRET_KEY = "NKm8TIP5kbNocOWlyjHELWrii+Y9rznKzJvulcnY"
VIDEO_UPLOAD_BUCKET = "cse546-project2-video-uploads-abaner40"

# Press Shift+F10 to execute it or replace it with your code.
# Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.


def handler(video_name):
    # Use a breakpoint in the code line below to debug your script.
    print("Inside handler")  # Press Ctrl+F8 to toggle the breakpoint.

    # video_name = event['Records'][0]['s3']['object']['key']

    session = Session(aws_access_key_id=ACCESS_KEY,
              aws_secret_access_key=SECRET_KEY)

    dir_prefix = video_name.split(".")[0] + "-"
    temp_dir = tempfile.mkdtemp(prefix = dir_prefix)

    video_file_path = temp_dir + "/" + video_name
    session.resource('s3').Bucket(VIDEO_UPLOAD_BUCKET).download_file(video_name, video_file_path)

    print(temp_dir)

    # print(folder_name)
    # folder_path = "/tmp/" + folder_name + "/"

    os.system("ffmpeg -i " + str(video_file_path) + " -r 1 " + str(temp_dir) +  "/" + "image-%3d.jpeg")

    image_names = []
    for filename in os.listdir(temp_dir):
        # img = cv2.imread(os.path.join(temp_dir, filename))
        # if img is not None:
            # images.append(img)
        if (filename.endswith(".jpeg")):
            print(filename)
            image_names.append(filename)

    print(image_names)

    encoding_variable = np.load("./encoding", allow_pickle=True)

    known_face_names = encoding_variable["name"]
    # print("=====")
    # print(known_face_names)
    # print("=====")
    known_face_encodings = encoding_variable["encoding"]
    # print("=====")
    # print(known_face_encodings)
    # print("=====")

    flag = False;

    name = ""

    for image_name in image_names:
        if flag == True:
            break;

        unknown_image = face_recognition.load_image_file(temp_dir + "/" + image_name)
        face_locations = face_recognition.face_locations(unknown_image)

        face_encodings = face_recognition.face_encodings(unknown_image, face_locations)

        # face_names = []
        recognized_name = ""
        for face_encoding in face_encodings:
            if flag == True:
                break

            # See if the face is a match for the known face(s)
            matches = face_recognition.compare_faces(known_face_encodings, face_encoding)
            # name = "Unknown"

            # # If a match was found in known_face_encodings, just use the first one.
            if True in matches:
                first_match_index = matches.index(True)
                recognized_name = known_face_names[first_match_index]
                flag = True

            # Or instead, use the known face with the smallest distance to the new face
            # face_distances = face_recognition.face_distance(known_face_encodings, face_encoding)
            # best_match_index = np.argmin(face_distances)
            # if matches[best_match_index]:
            #     name = known_face_names[best_match_index]

            # face_names.append(name)

    print(recognized_name)


    dynamodb_client = boto3.client('dynamodb', region_name="us-east-1")

    response = dynamodb_client.get_item(
        TableName=TABLE_NAME,
        Key={
            'name': {'S': recognized_name}
        }
    )

    print("===== Recognized Name =====")
    print(recognized_name)
    print("===== Response =====")
    print(response)

    s3_key = video_name.split('.')[0]
    s3_val = response['Item']['name']['S'] + ',' + response['Item']['major']['S'] + ',' + response['Item']['year']['S']

    print(s3_val)


    s3 = boto3.resource(
        's3',
        region_name = 'us-east-1',
        aws_access_key_id = ACCESS_KEY,
        aws_secret_access_key = SECRET_KEY
    )
    
    s3.Object('cse546-project2-classification-outputs-abaner40', s3_key).put(Body = s3_val)

    shutil.rmtree(temp_dir)    


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    handler('test_1.mp4')

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
