# chessboard

![chessboard screenshot](https://github.com/domgeorg/chessboard/blob/master/screenshot.png)

Given a square chessboard of N x N size, the position of Knight and position of a target is given. We need to find out minimum steps a Knight will take to reach the target position.

Shortest Path in almost all cases is a breadth first search implementation. (BFS). 

The idea is as follows: 
1. Start from the source location. 
2. Find all the possible locations Knight can move to from the given location. 
3. As long as the possible locations are inside the chessboard, add all such locations to a queue (to process one after the other) 
4. Perform steps 1 to 3 until queue is exhausted. 
5. If destination location is found in Step 2, report the location and the path. 

Note: 
1. To find all possible locations a Knight can jump to: 
I have used pythagorean theorem where a move makes a right angle triangle of one side to be of size 1 and second side to be of size 2 positions. So as per the theorem, the addition of squares of the 2 sides is equal to the square of 3rd side. (1^2 + 2^2 = 5) 

2. To track back shortest path: 
The chess board is created with Position Object which consists of X and Y co-ordinates and depth variable which says deep the location is from the source location. 
X and Y objects are the co-ordinates of the source location from where the jump was made. 
