package core.lexer.core.conversors;

import java.util.Arrays;

class Signature {
    final int currentPartition;
    final int[] targets;
    final int hashCode;

    Signature(int currentPartition, int[] stateTransitions, int[] partitionMap) {
        this.currentPartition = currentPartition;
        this.targets = new int[stateTransitions.length];
        int h = currentPartition;
        for (int i = 0; i < stateTransitions.length; i++) {
            int t = stateTransitions[i];
            int targetPid = (t == -1) ? -1 : partitionMap[t];
            this.targets[i] = targetPid;
            h = h * 31 + targetPid;
        }
        this.hashCode = h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Signature other = (Signature) o;
        if (currentPartition != other.currentPartition) return false;
        return Arrays.equals(targets, other.targets);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
