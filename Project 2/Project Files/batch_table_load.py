import json
import boto3



def load_data_into_dynamodb():
    dynamodb = boto3.resource('dynamodb')

    table = dynamodb.Table('cse546_student_data')
    items = []

    with open('./student_data.json', 'r') as json_file:
        data = json_file.read()

        objects = json.loads(data)

        print(data)

        for row in objects:
            # print(json.loads(row))
            # items.append(json.loads(row))
            print(row)
            items.append(row)

    print(items)
            
    with table.batch_writer() as batch:
        for item in items:
            batch.put_item(Item=item)

    # dynamodb = boto3.client('dynamodb')

    # with open('student_data.json', 'r') as datafile:
    #     records = json.load(datafile)

    # for data in records:
    #     print(data)
    #     item = {
    #             'id':{'S': data['id']},
    #             'name':{'S': data['name']},
    #             'major':{'S': data['major']},
    #             'year':{'S': data['year']}
    #     }
    #     print(item)
    #     response = dynamodb.put_item(
    #         TableName='cse546_student_data', 
    #         Item=item
    #     )
    #     print("UPLOADING ITEM")
    #     print(response)



if __name__ == '__main__':
    load_data_into_dynamodb()
