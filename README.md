<h2>Chord</h2>
<br>
This is implementation of Chord. Please check the [paper](http://db.cs.duke.edu/courses/cps212/spring15/15-744/S07/papers/chord.pdf) for more background information.

<br>
<h3>The implementation is good because...</h3>

1.	The search time efficiency is **O(log(N))** by maintaining an 32-entry finger table in each node.

2.	**It supports concurrent nodes joining and leaving. This is achieved by implementing the Stabilization part in the [Chord paper](http://db.cs.duke.edu/courses/cps212/spring15/15-744/S07/papers/chord.pdf).** The system will always converge to correct status even if it "*needs to deal with nodes joining the system concurrently and with nodes that fail or leave voluntarily*". Every node will keep communicating with its successor and correcting its finger table. This is implemented by multi-threading programming.

3.	Consistent SHA-1 hashing is used to give identifier to string and socket address.


<br>
<h3>How to compile and run</h3>

<br>
**Compile**

Open terminal, change directory to **Chord**.

	cd /Users/.../Chord

Now you could compile it using my Makefile! Just type `make`.
	
Hopefullly now you have all ***.class** files in your current directory.

<br>
**Run**

1.	Run Chord
	
	- Create a chord ring
		
			java Chord 8001
	  
	  Note here the C for Chord is uppercase, and 8050 is the port that you want this node to listen to.	
	  
	  Hopefully you are going to see something like this:
	  
	  		Joining the Chord ring.
			Local IP: 10.190.92.156

			You are listening on port 8001.
			Your position is 8459f9fa (51%).
			Your predecessor is yourself.
			Your successor is yourself.

	
	- Join an existing ring
	
			java Chord 8010 10.190.92.156 8001
	
	  This means you are creating a node that listen to port 8051 and it is joining a ring containing 10.190.92.156:8001.
	  
	 
	  If the input is right and the port 8010 is not occupied, you will see the following lines:
	  
	  
	  		Joining the Chord ring.
			Local IP: 10.190.92.156

			You are listening on port 8010.
			Your position is 1ac96434 (10%).
			Your predecessor is updating.
			Your successor is node /10.190.92.156, port 8001, position 8459f9fa (51%).
	  		
	  
	  After you create a node, you could input `info` at any time to check this node's socket address, predecessor and finger table. You could also terminate this node and leave chord ring by inputing `quit` or just press ctrl+C.

2.	Run Query
	
		java Query 10.190.92.156 8010
	
	If the program cannot connect to the node you are trying to contact, it will exit. Or if the connection is successful, you will see the following lines:
		
		Connection to node /10.190.92.156, port 8010, position 1ac96434 (10%).

		Please enter your search key (or type "quit" to leave):
	
	Then search anything you want! 
	
	Quit by inputing `quit` or just press ctrl+C.
	
<br>	
<h3>Programming details</h3>

The **Node.java** includes all core data structure and functionalities for chord node. While **Chord.java** and **Query.java** are main classes for chord and query respectively. **Helper.java** includes some useful methods including computation, hashing and network services. Other classes are threads will be run during a node's life cycle (e.g. listener thread, stabilize thread, etc.).

I added detailed comments to all source codes, so please check them if you'd like to. Also, please feel free to contact me if you need any other information. :)


<br>