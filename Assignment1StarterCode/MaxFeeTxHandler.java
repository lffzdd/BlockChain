import java.util.*;

public class MaxFeeTxHandler extends TxHandler {
    public MaxFeeTxHandler(UTXOPool utxoPool) {
        super(utxoPool);
    }

    /**
     * 计算交易费用
     */
    private double calculateFee(Transaction tx) {
        double inputSum = 0;
        double outputSum = 0;

        // 计算输入总和
        for (Transaction.Input input : tx.getInputs()) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output prevOutput = utxoPool.getTxOutput(utxo);
            if (prevOutput != null) {
                inputSum += prevOutput.value;
            }
        }

        // 计算输出总和
        for (Transaction.Output output : tx.getOutputs()) {
            outputSum += output.value;
        }

        return inputSum - outputSum;
    }

    /**
     * 使用动态规划找到最大费用的交易集合
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        if (possibleTxs == null || possibleTxs.length == 0) {
            return new Transaction[0];
        }

        // 计算每个交易的费用
        double[] fees = new double[possibleTxs.length];
        for (int i = 0; i < possibleTxs.length; i++) {
            fees[i] = calculateFee(possibleTxs[i]);
        }

        // 使用回溯法找到最大费用的有效交易集合
        ArrayList<Transaction> bestSet = new ArrayList<>();
        ArrayList<Transaction> currentSet = new ArrayList<>();
        double bestFee = 0;
        double currentFee = 0;

        // 尝试所有可能的交易组合
        for (int i = 0; i < possibleTxs.length; i++) {
            // 保存当前UTXO池的状态
            UTXOPool originalPool = new UTXOPool(utxoPool);

            // 尝试添加当前交易
            if (isValidTx(possibleTxs[i])) {
                currentSet.add(possibleTxs[i]);
                currentFee += fees[i];

                // 更新UTXO池
                for (Transaction.Input input : possibleTxs[i].getInputs()) {
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    utxoPool.removeUTXO(utxo);
                }
                byte[] txHash = possibleTxs[i].getHash();
                for (int j = 0; j < possibleTxs[i].numOutputs(); j++) {
                    Transaction.Output output = possibleTxs[i].getOutput(j);
                    UTXO utxo = new UTXO(txHash, j);
                    utxoPool.addUTXO(utxo, output);
                }

                // 如果当前集合的费用更高，更新最佳集合
                if (currentFee > bestFee) {
                    bestFee = currentFee;
                    bestSet = new ArrayList<>(currentSet);
                }

                // 递归尝试添加更多交易
                handleTxs(Arrays.copyOfRange(possibleTxs, i + 1, possibleTxs.length));
            }

            // 恢复UTXO池状态
            utxoPool = originalPool;
            currentSet.remove(currentSet.size() - 1);
            currentFee -= fees[i];
        }

        // 应用最佳交易集合
        for (Transaction tx : bestSet) {
            for (Transaction.Input input : tx.getInputs()) {
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                utxoPool.removeUTXO(utxo);
            }
            byte[] txHash = tx.getHash();
            for (int i = 0; i < tx.numOutputs(); i++) {
                Transaction.Output output = tx.getOutput(i);
                UTXO utxo = new UTXO(txHash, i);
                utxoPool.addUTXO(utxo, output);
            }
        }

        return bestSet.toArray(new Transaction[0]);
    }
}
