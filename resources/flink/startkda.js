const kinesis = require("aws-sdk/clients/kinesisanalyticsv2");

const kinesisanalyticsv2 = new kinesis({apiVersion: '2018-05-23', region: "eu-west-1"});

const params = {
    ApplicationName: serverless.service.custom.kdaApplicationName,
    RunConfiguration: {
        ApplicationRestoreConfiguration: {
            ApplicationRestoreType: "RESTORE_FROM_LATEST_SNAPSHOT"
        },
        FlinkRunConfiguration: {
            AllowNonRestoredState: true
        }
    }
};

kinesisanalyticsv2.startApplication(params, function(err, data) {
    if (err) console.log(err, err.stack); // an error occurred
    else     console.log(data);           // successful response
});