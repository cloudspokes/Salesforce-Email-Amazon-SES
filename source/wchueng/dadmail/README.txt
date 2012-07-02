Dadmail ("the Father of all Mails") is an email service which listens for email records being created on Salesforce and sends out the corresponding email messages using Amazon SES.

***Features***

1. Send messages with or without attachments. The attachments can be of any mime type supported by Salesforce.
2. Send short subject only messages (in this case the subject is also used as the body).
3. Send messages with subject + body + attachments.
4. Send to one or multiple recipients for To, CC, BCC.


*** Configuration ***

Salesforce
----------

Install the unmanaged package for dadmail: https://na12.salesforce.com/packaging/installPackage.apexp?p0=04tU000000018De

The custom objects AMS_Email__c and AMS_Email_Attachment__c are basically the same as spec'ed out on the challenge page. The only difference is I added a Body__c field to AMS_Email__c. And all AMS_Email__c fields are Text(255) except for From__c and Reply_To__c which are Email type. The reason why the recipient fields are not Email type also is because they are meant to be used as lists of comma separated email addresses (to support multiple recipients for each recipient type).

There's a tab for AMS_Email__c you can use to submit a simple message with no attachments to dadmail.

I also provided an Apex code snippet for setting up the push topic ("AllAMSEmails"). See: https://bitbucket.org/williamcheung/dadmail/src/e7d1e0f528c1/apex-code-to-create-push-topic.txt


Heroku
------

You can use Heroku environment variables to configure these credentials:

heroku config:add FORCE_LOGIN_SERVER=https://login.salesforce.com

heroku config:add FORCE_USERNAME=abc.def.com
heroku config:add FORCE_PASSWORD=xxx

heroku config:add FORCE_SECURITY_TOKEN=xxxxxxZOOAXDVgLy6UE14VL1
heroku config:add FORCE_CLIENT_ID=xxxxxxDx8IX8nP5TPHmuHnrifrDFntBRnJpq9nMWMMarGHelLf8d1GjuWIt82Uk37a0cQ74GG2HQmQxNBx0z6
heroku config:add FORCE_CLIENT_SECRET=xxxxxx781327562132

heroku config:add AWS_ACCESS_KEY=xxxxxx5YOFTH272TEWGQ
heroku config:add AWS_SECRET_KEY=xxxxxxPS8mCUJqSIL4qt4/BXBjx3eFOh40H8ICWj
heroku config:add AWS_SES_VERIFIED_SENDER=xxx@yyy.com

Note using environment variables is optional, although recommended by Pat Patterson (see http://wiki.developerforce.com/page/Getting_Started_with_the_Force.com_Streaming_API where he says: "Note that it is best practice to keep credentials such as OAuth client identifiers and secrets and API usernames and passwords out of source code.").

Because I was dinged for using environment variables in another Heroku-only challenge, I want to point out that for dadmail you can put the credentials directly in the code. Go to /dadmail/app/config/Config.java and replace the System.getenv calls with hard coded strings. See https://bitbucket.org/williamcheung/dadmail/src/e7d1e0f528c1/app/config/Config.java for details.


*** Deployment ***

Heroku
------

- Clone dadmail from Bitbucket:

git clone git@bitbucket.org:williamcheung/dadmail.git
cd dadmail

- Create your Heroku app if you haven't already:

heroku create --stack cedar

- Configure your environment variables on Heroku as above if you haven't already:

heroku config:add ...

- Push dadmail master to Heroku:

git push heroku master

-- dadmail is a Play 1.2.4 app so Heroku will automatically resolve dependencies, install, and run.


Local
-----

Make sure you have Play 1.2.4 installed and set up.

- After cloning dadmail as above, update the dependencies of the Play app so they get put in the lib dir:

play deps

- Set your environment variables (or put your credentials in Config.java) as explained above

...

- Run dadmail:

play run

- In dev mode only (not necessary in prod mode as deployed to Heroku), you need to kick the bootstrap job which starts listening on Salesforce:

http://localhost:9000

- If you want to load dadmail into Eclipse, generate the .project and .classpath files:

play eclipsify

-- Note that this results in absolute paths so the Eclipse files are only usable on your machine. You need to do this again if you switch machines. That's Play.


*** Error Handling ***

Once you have successfully subscribed to the "AllAMSEmails" push topic, any exceptions encountered will be logged to the console and dadmail will continue to listen for email records. Possible error causes could be from the Force REST API or Amazon SES, or invalid data in the "incoming" email records.


*** Performance Optimization ***

When dadmail authenticates to Salesforce for using the REST API, it caches the access token to save having to reauthenticate each time it calls the REST API. The token is cached for 11 hours as seen in https://bitbucket.org/williamcheung/dadmail/src/9c2b9cd0d536/app/services/ForceRestApi.java. After 11 hours, dadmail will reauthenticate.

Make sure in Salesforce you set your "Session Timeout" to 12 hours so that the token (session ID actually) doesn't expire on Salesforce sooner than it expires in the cache.


*** Limitations ***

dadmail uses the configured AWS_SES_VERIFIED_SENDER for the From and Reply-To fields in outgoing messages, even if the email record on Salesforce has different values for From and Reply-To. This is because Amazon SES requires verified From and Reply-To addresses and will fail with arbitrary addresses. However, I left the From and Reply-To fields in the custom object for future email providers which can support arbitrary addresses.


Happy Father's Day with dadmail,

William Cheung
Toronto
Fri June 15, 2012
