import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Check here https://github.com/zhaohuabing/ScroogeCoin/blob/master/src/main/java/TxHandler.java 
 */
public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    private boolean consumedCoinAvilable(Transaction.Input input) {
        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
        return utxoPool.contains(utxo);
    }

    private boolean coinConsumedMultipleTime(Transaction.Input input, Set<UTXO> uSet) {
        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
        return !uSet.add(utxo);
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
		double inputSum = 0;
		double outputSum = 0;
        Set<UTXO> claimedUTXO = new HashSet<UTXO>();

        List<Transaction.Input> inputs = tx.getInputs();
        for (Transaction.Input input : inputs){
            if(coinConsumedMultipleTime(input, claimedUTXO) ||
                !consumedCoinAvilable(input) || !verifyCoin(tx, input, inputs.indexOf(input))){
                return false;
            }
            UTXO utxo  = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            inputSum += output.value;
        }

        List<Transaction.Output> outputs = tx.getOutputs();
        for(Transaction.Output o : outputs) {
			if (o.value <= 0) {
				return false;
			}
            outputSum += o.value;
        }
        return outputSum > inputSum ? false : true;
    }

    private boolean verifyCoin(Transaction tx, Transaction.Input input, int a) {
        UTXO utxo  = new UTXO(input.prevTxHash, input.outputIndex);
        Transaction.Output output = utxoPool.getTxOutput(utxo);
         PublicKey key = output.address;
         return Crypto.verifySignature(key, tx.getRawDataToSign(a), input.signature);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     * @param possibleTxs
     * @return
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Transaction[] transactions = new Transaction[possibleTxs.length];
        int i = 0;
        for(Transaction t : possibleTxs){
            if(isValidTx(t)){
              transactions[i] = t;
              i++;  
              removeConsumedCoins(t);
              addNewCoins(t);
            }
        }
        return transactions;
    }

    /**
     * Remove consumed coin.
     * @param tx
     */
    private void removeConsumedCoins(Transaction tx) {
        List<Transaction.Input> inputList = tx.getInputs();
        for(int i = 0; i < inputList.size(); i++) {
            Transaction.Input currentInput = inputList.get(i);
            UTXO utxo = new UTXO(currentInput.prevTxHash, currentInput.outputIndex);
            utxoPool.removeUTXO(utxo);
        }
    }

    /**
     * Add new coin
     * @param tx
     */
    private void addNewCoins(Transaction tx) {
        List<Transaction.Output> outpuList = tx.getOutputs();
        for(int i = 0; i < outpuList.size(); i++) {
            Transaction.Output output = outpuList.get(i);
            UTXO utxo = new UTXO(tx.getHash(), i);
            utxoPool.addUTXO(utxo, output);
        }
    }


}
