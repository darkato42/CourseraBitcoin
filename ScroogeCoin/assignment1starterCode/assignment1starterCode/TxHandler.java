import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class TxHandler {

    private UTXOPool p;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        p = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        boolean ifValidOutputsClaimed = true;
        boolean ifValidInputSignatures = true;
        boolean ifNoDuplicateUtxo = true;
        boolean ifAllPositiveOutputValues = true;
        boolean ifPositiveNetSum = true;
        Set<UTXO> inputSet = new HashSet<UTXO>();
        double sum = 0;

        int i = 0;
        for (Transaction.Input input : tx.getInputs()) {
            UTXO claimedUtxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output claimedOutput = p.getTxOutput(claimedUtxo);
            
            // (1) validate claimed output in the pool
            ifValidOutputsClaimed = ifValidOutputsClaimed && p.contains(claimedUtxo);
            if (!ifValidOutputsClaimed) return false;

            // (2) validate signature
            PublicKey address = p.getTxOutput(claimedUtxo).address; // PublicKey is the address of this claimed output
            byte[] message = tx.getRawDataToSign(i);
            byte[] signature = input.signature;
            ifValidInputSignatures = ifValidInputSignatures && Crypto.verifySignature(address, message, signature);
            if (!ifValidInputSignatures) return false;

            inputSet.add(claimedUtxo);
            sum += claimedOutput.value;
            i++;
        }

        // (3) detect duplicate claimedUtxo
        ifNoDuplicateUtxo = inputSet.size() == tx.numInputs();
        if (!ifNoDuplicateUtxo) return false;

        for (Transaction.Output output : tx.getOutputs()) {
            // (4) only positive output values
            if (output.value < 0){
                ifAllPositiveOutputValues = false;
            }
            sum -= output.value;
        }
        if (!ifAllPositiveOutputValues) return false;

        // (5) input values greater than output values
        ifPositiveNetSum = (sum >= 0);
        if (!ifPositiveNetSum) return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)){
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO utxoToDel = new UTXO(input.prevTxHash, input.outputIndex);
                    p.removeUTXO(utxoToDel);
                }

                for (int outputIndex = 0; outputIndex < tx.numOutputs(); outputIndex++) {
                    UTXO utxo = new UTXO(tx.getHash(), outputIndex);
                    p.addUTXO(utxo, tx.getOutput(outputIndex));
                }
                validTxs.add(tx);
            }
        }
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
