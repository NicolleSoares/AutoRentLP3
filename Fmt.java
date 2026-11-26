package com.mycompany.testes;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Formatação de moeda e datas (dd/mm/aaaa) usada em todo o sistema. */
final class Fmt {
    static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Fmt() { }

    static String data(LocalDate d) { return d == null ? "—" : d.format(DATA); }

    static LocalDate parseData(String texto) { return LocalDate.parse(texto.trim(), DATA); }

    static String codigo(int id) { return String.format("#%04d", id); }
}
