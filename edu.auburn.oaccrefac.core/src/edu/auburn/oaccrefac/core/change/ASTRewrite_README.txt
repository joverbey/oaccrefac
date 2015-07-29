
Note: This is as of Eclipse CDT Version 8.7

Along this glorious road of working with CDT, I have come across some very
subtle details about using the ASTRewrite class that I struggled to discover.
This document describes some of those details and how to mitigate them. Some
of them are still unknown and are listed here so that you can not waste your
time trying to do them.

1. ASTRewrite objects have a nested behavior.
        Each ASTRewrite is associated with a node and the already-existing tree
        that the node consists of. Operations such as replace and insertBefore
        offer ways of replacing nodes within that associated node-tree as well
        as inserting into that associated node-tree, respectively.
        
        However, here's the catch --
        
        Those operations return a brand new ASTRewrite object to (as the documentation
        depicts, vaguely) 'further rewrite that node'. The documentation does not
        tell you exactly how they work, so here's how it goes:
        
        When you call replace to replace a node within the original node-tree, the
        operation returns a new ASTRewrite object. You CANNOT use the original rewriter
        to further modify this node, as it is not technically a part of the original
        node-tree. To modify this node any further, you must use the returned ASTRewrite
        object. This is how the 'nested' behavior works with CDT's ASTRewrite class. The
        insertBefore method works the same way, except for the inserted node.
        
        Unintuitive.
        
        You can find an example of how the nested behavior works in the 'doChange' method
        of 'core.change.StripMine.java'. You'll notice the inStrip_rewriter is used to
        further modify the in-strip loop after it has been placed into the original tree.

2. ASTRewrite operations that present new nodes (replace, insertBefore) cannot
   present frozen nodes.
        When first getting the node-tree from the translation unit, the parsing
        method 'freezes' the AST in order to maintain a degree of immutability. This
        can create a few minor headaches when dealing with replacing nodes and inserting
        new nodes into a tree. 
        
        For example, say you wanted to unroll a for-loop body. These means we need the body 
        to be in multiple places. However, we cannot simple 'insertBefore' the same nodes
        from the existing tree after themselves. Rather, we must COPY the nodes first and
        then place them into the tree.
        
        So you will see in multiple places where a node is copied, well this is the reason.
        
        A few subtle things about copying, however, is that it removes comments, pragmas, and
        any locations. Big problem, still haven't figured that one out.
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        