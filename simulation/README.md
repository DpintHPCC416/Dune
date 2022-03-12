# Directory Descriptions
- `hard-origin`: contains the code of evaluating the performance of origin column sketch, bitmap sketch and scatter sketch on RMT platform
- `soft`: contains the code of evaluating the performance of scatter sketch on soft switch

# How to run
- for each directory, you need to pack the code into jar file. And then, run the jar file
- you can use java -jar Simulationxxx.jar --help to understand the meaning of arguments.(xx can be replaced with `Ex` in `hard-origin`, or `ExSoft` in `soft`)
- especially, `--fin` requires you to identify the path of input flow information file, the file need to satisfy the format below:
  - the first line is an integer n representing the number of flow nums
  - each line from the 2nd to (n+1)th line represents information of a flow in the comma-seperated format, that is, `srcip,srcport,destip,destport,pps,real-packet`
  - pps means packet per second, real-packet is the real packet of a flow in real trace
- run with `java -jar Simulationxxx.jar args`