package com.example.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
            .map(ItemCarrinho::calcularSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        int totalQuantidade = itens.stream().mapToInt(ItemCarrinho::getQuantidade).sum();
        int descontoPorQuantidade;
        if (totalQuantidade <= 1) {
            descontoPorQuantidade = 0;
        } else if (totalQuantidade == 2) {
            descontoPorQuantidade = 5;
        } else if (totalQuantidade == 3) {
            descontoPorQuantidade = 7;
        } else {
            descontoPorQuantidade = 10;
        }

        int descontoPorCategoriaTotal = itens.stream().mapToInt(item -> {
            switch (item.getProduto().getCategoria()) {
            case CAPINHA:
                return 3 * item.getQuantidade();
            case CARREGADOR:
                return 5 * item.getQuantidade();
            case FONE:
                return 3 * item.getQuantidade();
            case PELICULA:
                return 2 * item.getQuantidade();
            case SUPORTE:
                return 2 * item.getQuantidade();
            default:
                return 0;
            }
        }).sum();

        int percentualTotal = descontoPorQuantidade + descontoPorCategoriaTotal;

        if (percentualTotal > 25) {
            percentualTotal = 25;
        }

        BigDecimal percentualDesconto = BigDecimal.valueOf(percentualTotal);

        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(valorDesconto).setScale(2, RoundingMode.HALF_UP);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }
}
