const {LambdaClient,UpdateFunctionConfigurationCommand} = require("@aws-sdk/client-lambda");
const lambdaClient = new LambdaClient({region:process.env.REGION});


exports.handler = async (event, context) => {
    console.log('Update memory event received', JSON.stringify(event));

    try {
        const { functionName, memory } = JSON.parse(Buffer.from(event.Records[0].kinesis.data, "base64").toString());
        
        await lambdaClient.send(new UpdateFunctionConfigurationCommand({FunctionName: functionName, MemorySize: memory.split(".")[0] }));

        console.log("Updated function memory");    
    } catch (error) {
        console.log("Error updating function memory", error);
    }
    
}
