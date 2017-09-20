import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    private HashMap<ByteArrayWrapper, Block> H;
    private HashMap<ByteArrayWrapper, UTXOPool> hmapUtxoPool;
    private TransactionPool txPool;
    private ByteArrayWrapper maxHeightBlockHash;
    private int maxHeight;
    private HashMap<ByteArrayWrapper, Integer> hmapHeight; 

    public static final int CUT_OFF_AGE = 10;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // Add the Genesis Block to HashMap.
        H = new HashMap<ByteArrayWrapper, Block>();
        ByteArrayWrapper hash = new ByteArrayWrapper(genesisBlock.getHash());
        H.put(hash, genesisBlock);

        // Initialise Transaction Pool
        txPool = new TransactionPool();
        // Initialise UTXO Pool
        UTXOPool utxoPool = new UTXOPool();
        Transaction coinbase = genesisBlock.getCoinbase();
        utxoPool.addUTXO(new UTXO(coinbase.getHash(), 0), coinbase.getOutput(0));
        hmapUtxoPool = new HashMap<ByteArrayWrapper, UTXOPool>();
        hmapUtxoPool.put(hash, utxoPool);
        // Initialise max height
        hmapHeight = new HashMap<ByteArrayWrapper, Integer>();
        maxHeightBlockHash = hash;
        maxHeight = 1;
        hmapHeight.put(hash, 1);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return H.get(maxHeightBlockHash);
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return hmapUtxoPool.get(maxHeightBlockHash);
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if (block.getPrevBlockHash() == null){
            return false;
        }

        // Check TXs. Return false when invalid TX found
        ByteArrayWrapper prevBlockHash = new ByteArrayWrapper(block.getPrevBlockHash());
        UTXOPool prevUtxoPool = hmapUtxoPool.get(prevBlockHash);
        if (prevUtxoPool == null){  // This failed me for so long! Test 6: Process a block with an invalid prevBlockHash
            return false;
        }
        TxHandler txHandler = new TxHandler(prevUtxoPool);
        ArrayList<Transaction> txs = block.getTransactions();
        Transaction[] txsArray = txs.toArray(new Transaction[txs.size()]);
        Transaction[] validTxsArray = txHandler.handleTxs(txsArray);
        if (validTxsArray.length != txsArray.length)
            return false;

        // Handle coinbase tx
        UTXOPool newUtxoPool = txHandler.getUTXOPool();
        Transaction coinbase = block.getCoinbase();
        newUtxoPool.addUTXO(new UTXO(coinbase.getHash(), 0), coinbase.getOutput(0));

        int blockHeight = hmapHeight.get(prevBlockHash) + 1;

        if(blockHeight > (this.maxHeight - CUT_OFF_AGE)) {
            ByteArrayWrapper hash = new ByteArrayWrapper(block.getHash());
            H.put(hash, block);
            hmapUtxoPool.put(hash, newUtxoPool);
            hmapHeight.put(hash, blockHeight);
            if (blockHeight > this.maxHeight) {
                maxHeight = blockHeight;
                maxHeightBlockHash = hash;
            }
            return true;
        }
        else{
            return false;
        }
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        txPool.addTransaction(tx);
    }
}
