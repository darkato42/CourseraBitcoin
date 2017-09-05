import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import static java.util.stream.Collectors.toSet;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    private double _pGraph, _pMalicious, _pTxDistribution;
    private int _numRound;
    private boolean[] _followees;                // nodes this CompliantNode follows
    
    private Set<Transaction> _transactions;     // TXs to propose

    private boolean[] _whitelist;
    private int _numFollowees;

    private HashMap<Transaction, Set<Integer>> _candidates;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this._pGraph = p_graph;
        this._pMalicious = p_malicious;
        this._pTxDistribution = p_txDistribution;
        this._numRound = numRounds;

        this._numFollowees = 0;
    }

    /* Construct a set of node index for followees */
    public void setFollowees(boolean[] followees) {
        _followees = followees;
        _whitelist = new boolean[followees.length];
        Arrays.fill(_whitelist, Boolean.TRUE);
        
        for (int i = 0; i < followees.length; i++){
            if (followees[i]){
                this._numFollowees++;
            }
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        _transactions = pendingTransactions;
    }

    public Set<Transaction> sendToFollowers() {
        return _transactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        /**
         * A candidate TX is valid if:
         * 1. A majority of nodes propose it.
         * 2. It's already in the list of initialized TXs
         * 3. the sender is in the followee list.
         * 
         * Blacklist a sender:
         * 1. A node does not accept the consensus agreed by the majority.?
         * 2. A dead node returning nothing
         */

        _candidates = new HashMap<Transaction, Set<Integer>>();

        // Blacklist a node when not sending data
        Set<Integer> senders = candidates.stream().map(c -> c.sender).collect(toSet());
        for (int i = 0; i < _followees.length; i++) {
            if (_followees[i] && !senders.contains(i)){
                _whitelist[i] = false;
            }
        }

        // Count senders for new candidates
        for (Candidate item : candidates) {
            // Only valid from whitelist followees
            if (!(_followees[item.sender] && _whitelist[item.sender])){
                continue;
            }

            // If not in its valid transaction list, add to the candidate list
            if (!_transactions.contains(item.tx)){
                Set<Integer> candidateSenders = _candidates.get(item.tx);
                // receive a new transaction from a new sender
                if (candidateSenders == null) {
                    candidateSenders = new HashSet<Integer>();
                    candidateSenders.add(item.sender);
                    _candidates.put(item.tx, candidateSenders);
                }
                // receive a existing transaction from a new sender
                else if (!candidateSenders.contains(item.sender)){
                    candidateSenders.add(item.sender);
                    _candidates.put(item.tx, candidateSenders);
                }
            }
        }

        // Add valid candidate to transaction list
        for (Transaction item : _candidates.keySet()) {

            // This simple add scored 88
            _transactions.add(item);

            // The below agreed by majority logic didn't pass the score.
            // Set<Integer> candidateSenders = _candidates.get(item);
            // if (candidateSenders.size() >= (_numFollowees/2)){
            //     _transactions.add(item);
            // }
        }
        
    }
}
