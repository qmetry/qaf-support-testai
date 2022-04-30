[![License](https://img.shields.io/github/license/qmetry/qaf-support-testai.svg)](http://www.opensource.org/licenses/mit-license.php)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.qmetry/qaf-support-testai/badge.svg)](https://mvnrepository.com/artifact/com.qmetry/qaf-support-testai/latest)
[![GitHub tag](https://img.shields.io/github/tag/qmetry/qaf-support-testai.svg)](https://github.com/qmetry/qaf-support-testai/tags)
[![javadoc](https://javadoc.io/badge2/com.qmetry/qaf-support-testai/javadoc.svg)](https://javadoc.io/doc/com.qmetry/qaf-support-testai)
# qaf-support-testai
qmetry automation framework testai support - adding intelligence with test.ai


 ### Usage: 
 - Add [qaf-support-testai dependency](https://mvnrepository.com/artifact/com.qmetry/qaf-support-testai/latest) to your project
 - Provide properties for test.ai integration. Assuming use of self descriptive locators no change required in your existing code!...
 - Run your test 
 
 Below are properties for testai
```
testai.api.key - test.ai api key
testai.server.url - server url, for example https://sdk.test.ai 

testai.training.mode - (optional) boolean default true, enable/disable training mode. 
```

### Limitations:
- Classification for Element list are not supported
- When element recovered by test.ai classification and element found by coordinates from classification using js, only limited commands supported. Supported commands in that case are: isPresent, GET_ELEMENT_TEXT, GET_ELEMENT_TAG_NAME, GET_ELEMENT_LOCATION, GET_ELEMENT_SIZE, GET_ELEMENT_RECT, CLICK, CLICK_ELEMENT, SEND_KEYS_TO_ELEMENT, SUBMIT_ELEMENT and respective wait/verify/assert methods.

In order to use test.ai you need to run your test(s) at least once in training mode. You can disable training mode after one or more execution in training mode to improve execution performance.
Refer test.ai documentation to get test.ai conceptual overview 

Here is [demo project](https://github.com/qmetry/qaf-support-testai/files/8595394/qaf-testai-demo.zip) to try this library.