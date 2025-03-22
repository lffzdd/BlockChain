import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

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

    /**
     *  @return true if:
     * (1) all outputs referenced by tx's inputs are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        if (tx == null)
            return false;

        // 使用HashSet来跟踪已使用的UTXO，避免重复使用
        HashSet<UTXO> usedUTXOs = new HashSet<>();
        double inputSum = 0;

        // 1. 验证所有输入
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);

            if (!utxoPool.contains(utxo)) // 检查UTXO是否在池中
                return false;

            if (!usedUTXOs.add(utxo)) // 检查UTXO是否已被本交易使用（防止双重支付）
                return false;

            // 获取对应的输出并验证签名
            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);
            if (!Crypto.verifySignature(prevOutput.address, tx.getRawDataToSign(i),
                    input.signature)) {
                return false;
            }

            inputSum += prevOutput.value;
        }

        // 2. 验证所有输出
        double outputSum = 0;
        for (int i = 0; i < tx.numOutputs(); i++) {
            Transaction.Output output = tx.getOutput(i);

            // 检查输出值是否为非负
            if (output.value < 0) {
                return false;
            }

            outputSum += output.value;
        }

        // 3. 验证输入总和是否大于等于输出总和
        return inputSum >= outputSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        if (possibleTxs == null)
            return new Transaction[0];

        ArrayList<Transaction> acceptedTxs = new ArrayList<>();

        /*        
         do {
            changed = false;
            for (每个交易) {
                if (交易有效) {
                    接受交易;
                    更新UTXO池;
                }
            }
        } while (本轮有新交易被接受); 
         
        */

        // 继续处理直到没有新的有效交易被添加
        boolean changed;
        do {
            changed = false;

            // 检查每个未处理的交易
            for (Transaction tx : possibleTxs) {
                if (tx == null || acceptedTxs.contains(tx))
                    continue;

                // 验证交易是否有效
                if (isValidTx(tx)) {
                    acceptedTxs.add(tx);
                    changed = true;

                    // 更新UTXO池：
                    // 1. 移除已使用的UTXO
                    for (Transaction.Input input : tx.getInputs()) {
                        UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                        utxoPool.removeUTXO(utxo);
                    }

                    // 2. 添加新的UTXO
                    byte[] txHash = tx.getHash();
                    for (int i = 0; i < tx.numOutputs(); i++) {
                        Transaction.Output output = tx.getOutput(i);
                        UTXO utxo = new UTXO(txHash, i);
                        utxoPool.addUTXO(utxo, output);
                    }
                }
            }
        } while (changed); // 如果这轮有新交易被接受，继续处理

        // 将ArrayList转换为数组返回
        return acceptedTxs.toArray(new Transaction[0]);
    }

}
