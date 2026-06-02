package tn.iset.investplatformpfe.Service;

public class ScoredService<T> {

    private T service;
    private int score;

    public ScoredService(T service, int score) {
        this.service = service;
        this.score = score;
    }

    public T getService() {
        return service;
    }

    public int getScore() {
        return score;
    }
}