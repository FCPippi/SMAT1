package com.sma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class Simulador {
    private double tempoGlobal;
    private GeradorNumPseudoaleatorio geradorNumPseudoaleatorio = new GeradorNumPseudoaleatorio();
    private ArrayList<Evento> scheduler = new ArrayList<>();
    private ArrayList<Fila> filas = new ArrayList<>();
    private ArrayList<App.Ligacao> ligacoes = new ArrayList<>();
    private Queue<Double> listPseudoNum = new LinkedList<>();

    public void iniciaSimulacao(App.Config configSimulador) {
        tempoGlobal = 0;
        geradorNumPseudoaleatorio.semente = configSimulador.getSemente();
        int count = configSimulador.getQtdNumPseudoAleatorios();
        while (count > 0) {
            listPseudoNum.add(geradorNumPseudoaleatorio.gerarNumPseudoaleatorio());
            count--;
        }

        iniciaFilas(configSimulador);
        Evento primeiro = new Evento(configSimulador.getChegada(), TipoEvento.CHEGADA, configSimulador.getIdFilaChegada());
        scheduler.add(primeiro);

        while (!listPseudoNum.isEmpty()) {
            Evento evento = getNextEvento();
            switch (evento.getTipo()) {
                case CHEGADA -> chegada(evento);
                case SAIDA -> saida(evento);
                case PASSAGEM -> passagem(evento);
            }
        }
        printaSimulacao();
    }

    private void iniciaFilas(App.Config configSimulador) {
        for (App.ConfigFila novaFila : configSimulador.getFilas()) {
            Fila fila;
            if (novaFila.getTempoChegadaMin() == -1 || novaFila.getTempoChegadaMax() == -1) {
                fila = new Fila(novaFila.getServidores(), novaFila.getCapacidade(), novaFila.getIdFila(), novaFila.getTempoAtendimentoMin(), novaFila.getTempoAtendimentoMax());
            } else {
                fila = new Fila(novaFila.getServidores(), novaFila.getCapacidade(), novaFila.getIdFila(), novaFila.getTempoChegadaMin(), novaFila.getTempoChegadaMax(), novaFila.getTempoAtendimentoMin(), novaFila.getTempoAtendimentoMax());
            }
            if (novaFila.getProbabilidadeSaida() != -1) {
                fila.setProbabilidadeSaida(novaFila.getProbabilidadeSaida());
            }
            filas.add(fila);
        }

        ligacoes.addAll(configSimulador.getLigacoes());
    }

    private void chegada(Evento evento) {
        Fila filaEvento = filas.get(evento.getIdFilaOrigem());
        somaTempo(evento);
        if (filaEvento.cabeMaisUmElemento()) {
            filaEvento.push("x");
            if (filaEvento.getStatus() <= filaEvento.getServidores()) {
                int idDestino = getDestino(filaEvento.getIdFila());
                Evento aux = new Evento(0, idDestino >= 0 ? TipoEvento.PASSAGEM : TipoEvento.SAIDA, evento.getIdFilaOrigem());
                aux.setIdFilaDestino(idDestino);
                agendarEvento(aux);
            }
        } else {
            filaEvento.addPerda();
        }
        Evento aux = new Evento(0, TipoEvento.CHEGADA, evento.getIdFilaOrigem());
        aux.setIdFilaDestino(evento.getIdFilaOrigem());
        agendarEvento(aux);
    }

    private void saida(Evento evento) {
        Fila filaEvento = filas.get(evento.getIdFilaOrigem());
        somaTempo(evento);
        filaEvento.pop();
        if (filaEvento.getStatus() >= filaEvento.getServidores()) {
            int idDestino = getDestino(filaEvento.getIdFila());
            Evento aux = new Evento(0, idDestino >= 0 ? TipoEvento.PASSAGEM : TipoEvento.SAIDA, evento.getIdFilaOrigem());
            aux.setIdFilaDestino(idDestino);
            agendarEvento(aux);
        }
    }

    private void passagem(Evento evento) {
        Fila filaEventoDestino = filas.get(evento.getIdFilaDestino());
        Fila filaEventoOrigem = filas.get(evento.getIdFilaOrigem());
        somaTempo(evento);
        filaEventoOrigem.pop();
        if (filaEventoOrigem.getStatus() >= filaEventoOrigem.getServidores()) {
            int idDestino = getDestino(filaEventoOrigem.getIdFila());
            Evento aux = new Evento(0, idDestino >= 0 ? TipoEvento.PASSAGEM : TipoEvento.SAIDA, evento.getIdFilaOrigem());
            aux.setIdFilaDestino(idDestino);
            agendarEvento(aux);
        }
        if (filaEventoDestino.cabeMaisUmElemento()) {
            filaEventoDestino.push("x");
            if (filaEventoDestino.getStatus() <= filaEventoDestino.getServidores()) {
                int idDestino = getDestino(filaEventoDestino.getIdFila());
                Evento aux = new Evento(0, idDestino >= 0 ? TipoEvento.PASSAGEM : TipoEvento.SAIDA, evento.getIdFilaDestino());
                aux.setIdFilaDestino(idDestino);
                agendarEvento(aux);
            }
        } else {
            filaEventoDestino.addPerda();
        }
    }

    private void agendarEvento(Evento evento) {
        scheduler.add(calcularTempoAgendado(evento));
    }

    private Evento calcularTempoAgendado(Evento evento) {
        Fila filaEvento = filas.get(evento.getIdFilaOrigem());
        double numPseudoaleatorio = geradorNumPseudoaleatorio.gerarNumPseudoaleatorio();
        double tempo = switch (evento.getTipo()) {
            case CHEGADA -> filaEvento.getTempoChegadaMin() + ((filaEvento.getTempoChegadaMax() - filaEvento.getTempoChegadaMin()) * numPseudoaleatorio);
            case PASSAGEM, SAIDA -> filaEvento.getTempoAtendimentoMin() + ((filaEvento.getTempoAtendimentoMax() - filaEvento.getTempoAtendimentoMin()) * numPseudoaleatorio);
        };
        evento.setTempo(tempoGlobal + tempo);
        return evento;
    }

    private Evento getNextEvento() {
        Evento proximo = scheduler.get(0);
        int index = 0;
        for (int indice = 0; indice < scheduler.size(); indice++) {
            if (proximo.getTempo() > scheduler.get(indice).getTempo()) {
                proximo = scheduler.get(indice);
                index = indice;
            }
        }
        scheduler.remove(index);
        return proximo;
    }

    private int getDestino(int id) {
        ArrayList<App.Ligacao> listAux = new ArrayList<>();
        double rand = listPseudoNum.poll();
        double sum = 0.0;
        for (var ligacao : ligacoes) {
            if (ligacao.getIdOrigem() == id) {
                listAux.add(ligacao);
            }
        }
        Collections.sort(listAux, Comparator.comparing(App.Ligacao::getProbabilidade));
        for (var itemList : listAux) {
            sum += itemList.getProbabilidade();
            if (rand < sum) {
                return itemList.getIdDestino();
            }
        }
        return -1;
    }

    private void somaTempo(Evento evento) {
        double delta = evento.getTempo() - tempoGlobal;
        for (Fila fila : filas) {
            int status = fila.getStatus();
            fila.getAcumulador()[status] += delta;
        }
        tempoGlobal = evento.getTempo();
    }

    private void printaSimulacao() {
        System.out.println("Tempo Global: " + tempoGlobal + "\n");
        System.out.println("Escalonador: ");
        System.out.println(scheduler.toString());
        System.out.println("\n");
        imprimirEstatisticasDeCadaFila();
    }

    private void imprimirEstatisticasDeCadaFila() {
        for (Fila fila : filas) {
            System.out.println("Fila " + fila.getIdFila());
            System.out.println("\t" + "Dados por estado");
            for (int estado = 0; estado < 6; estado++) {
                System.out.println("Estado " + estado);
                System.out.println("Probabilidade: " + (fila.probabilidadeDoEstado(estado, tempoGlobal) * 100) + "%");
                System.out.println("População: " + fila.populacaoDoEstado(estado, tempoGlobal) + " clientes");
                System.out.println("Vazão: " + fila.vazaoDoEstadoPorHora(estado, tempoGlobal) + " clientes por hora");
                System.out.println("Utilização: " + (fila.utilizacaoDoEstado(estado, tempoGlobal) * 100) + "%");
            }
            System.out.println("Totais");
            System.out.println("População: " + fila.populacao(tempoGlobal) + " clientes");
            System.out.println("Vazão: " + fila.vazaoPorHora(tempoGlobal) + " clientes por hora");
            System.out.println("Utilização: " + (fila.utilizacao(tempoGlobal) * 100) + "%");
            System.out.println("Demora: " + fila.tempoDeRespostaEmHoras(tempoGlobal) + " hora(s)");
            System.out.println("Loss: " + fila.getLoss());
        }
        System.out.println(tempoGlobal);
    }

    public static class Fila {
        private ArrayList<String> elementos;
        private int servidores;
        private int capacidade;
        private int idFila;
        private double tempoChegadaMin;
        private double tempoChegadaMax;
        private double tempoAtendimentoMin;
        private double tempoAtendimentoMax;
        private double probabilidadeSaida;
        private double[] acumulador;
        private int perdas;

        public Fila(int servidores, int capacidade, int idFila, double tempoChegadaMin, double tempoChegadaMax, double tempoAtendimentoMin, double tempoAtendimentoMax) {
            elementos = new ArrayList<>();
            this.idFila = idFila;
            this.servidores = servidores;
            this.capacidade = capacidade;
            acumulador = new double[this.capacidade + 1];
            if (capacidade == -1) {
                acumulador = new double[100];
            }
            this.tempoChegadaMin = tempoChegadaMin;
            this.tempoChegadaMax = tempoChegadaMax;
            this.tempoAtendimentoMin = tempoAtendimentoMin;
            this.tempoAtendimentoMax = tempoAtendimentoMax;
            perdas = 0;
        }

        public Fila(int servidores, int capacidade, int idFila, double tempoAtendimentoMin, double tempoAtendimentoMax) {
            this(servidores, capacidade, idFila, -1, -1, tempoAtendimentoMin, tempoAtendimentoMax);
        }

        public ArrayList<String> getElementos() { return elementos; }
        public double[] getAcumulador() { return acumulador; }
        public int getServidores() { return servidores; }
        public void setServidores(int servidores) { this.servidores = servidores; }
        public int getCapacidade() { return capacidade; }
        public void setCapacidade(int capacidade) { this.capacidade = capacidade; }
        public int getStatus() { return elementos.size(); }
        public int getIdFila() { return idFila; }
        public double getTempoAtendimentoMin() { return tempoAtendimentoMin; }
        public double getTempoAtendimentoMax() { return tempoAtendimentoMax; }
        public double getTempoChegadaMin() { return tempoChegadaMin; }
        public double getTempoChegadaMax() { return tempoChegadaMax; }
        public double getProbabilidadeSaida() { return probabilidadeSaida; }
        public void setProbabilidadeSaida(double probabilidadeSaida) { this.probabilidadeSaida = probabilidadeSaida; }
        public void addPerda() { perdas++; }
        public int getLoss() { return perdas; }
        public boolean cabeMaisUmElemento() { return getStatus() < getCapacidade() || getCapacidade() == -1; }
        public boolean temServidoresOciosos() { return getStatus() < getServidores(); }
        public int push(String elemento) { elementos.add(elemento); return elementos.size(); }
        public String pop() { return elementos.remove(0); }

        public HashMap<Integer, Double> probabilidadesDeCadaEstado(double tempoGlobal) {
            HashMap<Integer, Double> probabilidades = new HashMap<>();
            for (int estado = 0; estado < getAcumulador().length; estado++)
                probabilidades.put(estado, probabilidadeDoEstado(estado, tempoGlobal));
            return probabilidades;
        }

        public double probabilidadeDoEstado(int estado, double tempoGlobal) {
            return getAcumulador()[estado] / tempoGlobal;
        }

        public double taxaDeAtendimentoDoEstadoPorHora(int estado) {
            return Math.min(estado, capacidade) * taxaMediaDeAtendimentoDaFilaPorHora();
        }

        public double taxaMediaDeAtendimentoDaFila() {
            return capacidade / (tempoAtendimentoMax - tempoAtendimentoMin);
        }

        public double taxaMediaDeAtendimentoDaFilaPorHora() {
            return taxaMediaDeAtendimentoDaFila() * 60;
        }

        public double populacaoDoEstado(int estado, double tempoGlobal) {
            return probabilidadeDoEstado(estado, tempoGlobal) * estado;
        }

        public double populacao(double tempoGlobal) {
            double populacao = 0;
            for (int estado = 0; estado < getAcumulador().length; estado++)
                populacao += populacaoDoEstado(estado, tempoGlobal);
            return populacao;
        }

        public double vazaoDoEstadoPorHora(int estado, double tempoGlobal) {
            return probabilidadeDoEstado(estado, tempoGlobal) * taxaDeAtendimentoDoEstadoPorHora(estado);
        }

        public double vazaoPorHora(double tempoGlobal) {
            double vazao = 0;
            for (int estado = 0; estado < getAcumulador().length; estado++)
            vazao += vazaoDoEstadoPorHora(estado, tempoGlobal);
            return vazao;
        }

        public double utilizacaoDoEstado(int estado, double tempoGlobal) {
            return probabilidadeDoEstado(estado, tempoGlobal) * (Math.min(estado, capacidade) / (double) capacidade);
        }

        public double utilizacao(double tempoGlobal) {
            double utilizacao = 0;
            for (int estado = 0; estado < getAcumulador().length; estado++)
                utilizacao += utilizacaoDoEstado(estado, tempoGlobal);
            return utilizacao;
        }

        public double tempoDeRespostaEmHoras(double tempoGlobal) {
            return populacao(tempoGlobal) / vazaoPorHora(tempoGlobal);
        }
    }

    public static class Evento {
        private double tempo;
        private TipoEvento tipo;
        private int idFilaOrigem;
        private int idFilaDestino;

        public Evento(double tempo, TipoEvento tipo, int idFilaOrigem) {
            this.tempo = tempo;
            this.tipo = tipo;
            this.idFilaOrigem = idFilaOrigem;
        }

        public double getTempo() { return tempo; }
        public void setTempo(double tempo) { this.tempo = tempo; }
        public TipoEvento getTipo() { return tipo; }
        public void setTipo(TipoEvento tipo) { this.tipo = tipo; }
        public int getIdFilaOrigem() { return idFilaOrigem; }
        public void setIdFilaOrigem(int idFilaOrigem) { this.idFilaOrigem = idFilaOrigem; }
        public int getIdFilaDestino() { return idFilaDestino; }
        public void setIdFilaDestino(int idFilaDestino) { this.idFilaDestino = idFilaDestino; }
    }

    public static class GeradorNumPseudoaleatorio {
        double a = 65;
        double c = 518;
        double M = 91481295;
        double semente = 7;

        public double gerarNumPseudoaleatorio() {
            semente = ((a * semente + c) % M);
            return normalizar();
        }

        public double normalizar() {
            return (1.0 * semente) / M;
        }
    }

    public enum TipoEvento {
        CHEGADA, SAIDA, PASSAGEM;
    }
}