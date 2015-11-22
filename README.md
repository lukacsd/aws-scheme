## AWS Scheme

An Android app that can be used to start services in AWS described by tags (it's got nothing to do with scaling, whatsoever).
Also, some functionality to display billing totals have been added. The data is fetched from the account's S3 billing bucket.

I wrote this a while ago to gain some insight in Android development. Also, because I'm a cheapo. I've got a site in AWS that doesn't need to be constantly online. In order to dodge the monthly bill, I have the site's domain name resolved to an elastic ip, and have that ip assigned to a facade that only gives a friendly bounce message. When I feel like showing off, I can use the app to switch the formation behind the elastic ip.

The infrastructure layout is driven by only two tags:
* one groups the instances together
* the other directs an ip to a network interface.

### Example

Assume, we have the below Ec2 resources and an elastic ip: 1.2.3.4 - *example.org* resolves to 1.2.3.4

| resource | tag | relationship |
|----------|-----|------|
| i-10000000	| scheme: facade	|	| 
| i-20000000	| scheme: livesite	|	|
| i-20000001	| scheme: livesite	|	|
| eni-10000000	| public-ip: 1.2.3.4	| attached to **i-10000000**	|
| eni-20000000	|	| attached to **i-20000000**	|
| eni-20000001	| public-ip: 1.2.3.4	| attached to **i-20000001**	|

Initially, *i-10000000* is running, having *1.2.3.4* associated with it, hence serving requests for *example.org*.
On switching the scheme to **livesite**, *i-20000000* and *i-20000001* are spinned up, ip is assigned to the new eni, then *i-10000000* is shut down.

### Authorization
The app needs to have access to Ec2 functionality in the targeted account. I have an IAM user with the following policy:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Action": "ec2:*",
            "Effect": "Allow",
            "Resource": "*"
        }
    ]
}
```

S3 is optional, but if it's needed the user needs to be permissioned:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:Get*",
        "s3:List*"
      ],
      "Resource": "*"
    }
  ]
}
```

To apply the above, save the credentials in a property file named something like: **account_< ARBITRARY_STRING >.properties**
```
accountId=123456    # display only
accountAlias=blah   # display only
accessKey=<IAM_ACCESS_KEY>
secretKey=<IAM_SECRET_KEY>
ec2.region=<AWS_ENDPOINT>   # e.g https://ec2.eu-central-1.amazonaws.com
s3.region=<AWS_REGION_NAME> # e.g eu-central-1
s3.billingBucket=<S3_BUCKET_NAME>
```

### Library dependencies
* commons-logging-1.1.1.jar
* gson-2.2.4.jar
* jackson-core-2.1.1.jar
* joda-time-2.8.2.jar
* aws-android-sdk-core-2.2.7.jar
* aws-android-sdk-ec2-2.2.7.jar
* *aws-android-sdk-s3-2.2.7.jar*

### License

All code is released under [Apache License v2](http://www.apache.org/licenses/LICENSE-2.0.html).
