package com.sma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.*;
import java.util.List;

public class App {

    public static void main(String[] args) throws Exception {
        Simulador simulador = new Simulador();
        Config configSimulador = lerArquivoYaml();
        simulador.iniciaSimulacao(configSimulador);
    }

    public static Config lerArquivoYaml() throws IOException {
        var mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File("config.yml"), Config.class);
    }

    public static class Config {
        private int qtdPseudoNum;
        private int semente;
        private double chegada;
        private int idFilaChegada;
        private List<ConfigFila> filas;
        private List<Ligacao> ligacoes;

        public int getQtdNumPseudoAleatorios() { return qtdPseudoNum; }
        public void setQtdNumPseudoAleatorios(int qtdPseudoNum) { this.qtdPseudoNum = qtdPseudoNum; }
        public int getSemente() { return semente; }
        public void setSemente(int semente) { this.semente = semente; }
        public double getChegada() { return chegada; }
        public int getIdFilaChegada() { return idFilaChegada; }
        public void setChegada(double chegada) { this.chegada = chegada; }
        public List<ConfigFila> getFilas() { return filas; }
        public void setFilas(List<ConfigFila> filas) { this.filas = filas; }
        public List<Ligacao> getLigacoes() { return ligacoes; }
        public void setConexoes(List<Ligacao> ligacoes) { this.ligacoes = ligacoes; }
    }

    public static class ConfigFila {
        private int servidores;
        private int capacidade;
        private int idFila;
        private double tempoChegadaMin;
        private double tempoChegadaMax;
        private double tempoAtendimentoMin;
        private double tempoAtendimentoMax;
        private double probabilidadeSaida;

        public int getServidores() { return servidores; }
        public int getCapacidade() { return capacidade; }
        public int getIdFila() { return idFila; }
        public double getTempoAtendimentoMin() { return tempoAtendimentoMin; }
        public double getTempoAtendimentoMax() { return tempoAtendimentoMax; }
        public double getTempoChegadaMin() { return tempoChegadaMin; }
        public double getTempoChegadaMax() { return tempoChegadaMax; }
        public double getProbabilidadeSaida() { return probabilidadeSaida; }
    }

    public static class Ligacao {
        private int idOrigem;
        private int idDestino;
        private double probabilidade;
    
        public Ligacao() {
        }
    
        public Ligacao(int idOrigem, int idDestino, double probabilidade) {
            this.idOrigem = idOrigem;
            this.idDestino = idDestino;
            this.probabilidade = probabilidade;
        }
    
        public int getIdDestino() { return idDestino; }
        public int getIdOrigem() { return idOrigem; }
        public double getProbabilidade() { return probabilidade; }
    }
}