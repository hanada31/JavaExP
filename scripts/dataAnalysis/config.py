id = 106


if id ==1:
    VERSIONS = ['1.0', '1.1'  , '1.2', '1.3', '1.4', '2.0', '2.1', '2.2', '2.3', '2.4', '2.5', '2.6', '2.7', '2.8.0', '2.9.0', '2.10.0', '2.11.0', '2.12.0', '2.13.0']
    TAG = 'apache-results-intra'
    FILE = lambda v, t: f'original_data/{t}/commons-io-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==2:
    VERSIONS = ['1.0', '1.1'  , '1.2', '1.3', '1.4', '2.0', '2.1', '2.2', '2.3', '2.4', '2.5', '2.6', '2.7', '2.8.0', '2.9.0', '2.10.0', '2.11.0', '2.12.0', '2.13.0']
    TAG = 'apache-results-inter'
    FILE = lambda v, t: f'original_data/{t}/commons-io-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'


if id ==3:
    VERSIONS = ['21', '22', '23', '24', '25', '26', '27', '28', '29', '30']
    TAG = 'android-results-intra'
    FILE = lambda v, t: f'original_data/{t}/framework-{v}/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==4:
    VERSIONS = ['21', '22', '23', '24', '25', '26', '27', '28', '29', '30']
    TAG = 'android-results-inter'
    FILE = lambda v, t: f'original_data/{t}/framework-{v}/exceptionInfo/exceptionConditionsOfAll.txt'


if id ==5:
    VERSIONS = ['10.0', '11.0', '12.0', '13.0', '14.0', '15.0', '16.0', '17.0', '18.0', '19.0', '20.0', '21.0', '22.0', '23.0']
    TAG = 'guava-results-intra'
    FILE = lambda v, t: f'original_data/{t}/guava-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==6:
    VERSIONS = ['10.0', '11.0', '12.0', '13.0', '14.0', '15.0', '16.0', '17.0', '18.0', '19.0', '20.0', '21.0', '22.0', '23.0']
    TAG = 'guava-results-inter'
    FILE = lambda v, t: f'original_data/{t}/guava-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'
    

if id ==7:
    VERSIONS = ['1.0', '1.1.1', '1.2', '1.3', '2.0']
    TAG = 'gs-core-results-intra'
    FILE = lambda v, t: f'original_data/{t}/gs-core-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==8:
    VERSIONS = ['1.0', '1.1.1', '1.2', '1.3', '2.0']
    TAG = 'gs-core-results-inter'
    FILE = lambda v, t: f'original_data/{t}/gs-core-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'
    
   
if id ==9:
    VERSIONS = ['0.9.0', '1.0.0', '1.1.0', '1.2.0', '1.3.0', '1.4.0', '1.5.0']
    TAG = 'jgrapht-core-results-intra'
    FILE = lambda v, t: f'original_data/{t}/jgrapht-core-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==10:
    VERSIONS = ['0.9.0', '1.0.0', '1.1.0', '1.2.0', '1.3.0', '1.4.0', '1.5.0']
    TAG = 'jgrapht-core-results-inter'
    FILE = lambda v, t: f'original_data/{t}/jgrapht-core-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'


if id ==11:
    VERSIONS = ['15.0', '15.1', '15.2', '15.3', '15.4']
    TAG = 'nashorn-core-results-intra'
    FILE = lambda v, t: f'original_data/{t}/nashorn-core-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==12:
    VERSIONS = ['15.0', '15.1', '15.2', '15.3', '15.4']
    TAG = 'nashorn-core-results-inter'
    FILE = lambda v, t: f'original_data/{t}/nashorn-core-{v}.jar/exceptionInfo/exceptionConditionsOfAll.txt'
    
    
if id ==101:
    VERSIONS = ['0.1', '0.2']
    TAG = 'rq2-commonIO'
    FILE = lambda v, t: f'original_data/{t}/commonIO-{v}/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==102:
    VERSIONS = ['0.1', '0.2']
    TAG = 'rq2-guava'
    FILE = lambda v, t: f'original_data/{t}/guava-{v}/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==103:
    VERSIONS = ['0.1', '0.2']
    TAG = 'rq2-jgrapht'
    FILE = lambda v, t: f'original_data/{t}/jgrapht-{v}/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==104:
    VERSIONS = ['0.1', '0.2']
    TAG = 'rq2-nashorn'
    FILE = lambda v, t: f'original_data/{t}/nashorn-{v}/exceptionInfo/exceptionConditionsOfAll.txt'
    
if id ==105:
    VERSIONS = ['0.1', '0.2']
    TAG = 'rq2-android'
    FILE = lambda v, t: f'original_data/{t}/android-{v}/exceptionInfo/exceptionConditionsOfAll.txt'
    
    
if id ==106:
    VERSIONS = ['0.1', '0.2']
    TAG = 'rq2-gs-core'
    FILE = lambda v, t: f'original_data/{t}/gs-core-{v}/exceptionInfo/exceptionConditionsOfAll.txt'
    