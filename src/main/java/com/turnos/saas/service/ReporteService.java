package com.turnos.saas.service;

import com.turnos.saas.dto.response.Responses.*;
import com.turnos.saas.model.enums.TurnoEstado;
import com.turnos.saas.repository.CotizacionRepository;
import com.turnos.saas.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final TurnoRepository turnoRepository;
    private final CotizacionRepository cotizacionRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID empresaId, LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        LocalDate fechaDesde = desde != null ? desde : inicioMes;
        LocalDate fechaHasta = hasta != null ? hasta : finMes;

        long turnosHoy = turnoRepository.countByEmpresaIdAndFecha(empresaId, hoy);

        List<TurnoEstado> activos = List.of(
                TurnoEstado.CONFIRMADO, TurnoEstado.PROGRAMADO, TurnoEstado.FINALIZADO, TurnoEstado.CANCELADO,
                TurnoEstado.SOLICITADO, TurnoEstado.EN_COTIZACION, TurnoEstado.COTIZADO
        );
        var turnosMes = turnoRepository.findByEmpresaIdAndEstadoInAndFechaRange(
                empresaId, activos, fechaDesde, fechaHasta);

        long turnosMesCount = turnosMes.size();
        long cancelados = turnosMes.stream().filter(t -> t.getEstado() == TurnoEstado.CANCELADO).count();
        double tasaCancelacion = turnosMesCount > 0 ? (double) cancelados / turnosMesCount * 100 : 0;

        BigDecimal ingresos = cotizacionRepository.findAll().stream()
                .filter(c -> c.getTurno().getEmpresa().getId().equals(empresaId))
                .map(c -> c.getPrecio())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, long[]> servicioStats = new LinkedHashMap<>();
        turnosMes.stream()
                .filter(t -> t.getServicio() != null && t.getEstado() != TurnoEstado.CANCELADO)
                .forEach(t -> {
                    String nombre = t.getServicio().getNombre();
                    servicioStats.computeIfAbsent(nombre, k -> new long[]{0});
                    servicioStats.get(nombre)[0]++;
                });

        List<TopServicioItem> topServicios = servicioStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .map(e -> new TopServicioItem(e.getKey(), e.getValue()[0], BigDecimal.ZERO))
                .collect(Collectors.toList());

        return new DashboardResponse(turnosHoy, turnosMesCount, cancelados,
                Math.round(tasaCancelacion * 100.0) / 100.0, ingresos, topServicios);
    }

    @Transactional(readOnly = true)
    public Page<TurnoSummary> getTurnos(UUID empresaId, LocalDate desde, LocalDate hasta, Pageable pageable) {
        return turnoRepository.findByFilters(empresaId, null, null, null, pageable)
                .map(t -> new TurnoSummary(
                        t.getId(),
                        t.getFechaConfirmada(),
                        t.getHoraConfirmada(),
                        t.getEstado(),
                        t.getCliente() != null ? t.getCliente().getNombre() : null,
                        t.getServicio() != null ? t.getServicio().getNombre() : null
                ));
    }

    @Transactional(readOnly = true)
    public List<TopServicioItem> getTopServicios(UUID empresaId, LocalDate desde, LocalDate hasta) {
        List<TurnoEstado> estadosActivos = List.of(
                TurnoEstado.CONFIRMADO, TurnoEstado.PROGRAMADO, TurnoEstado.FINALIZADO
        );
        var turnos = turnoRepository.findByEmpresaIdAndEstadoInAndFechaRange(empresaId, estadosActivos, desde, hasta);

        Map<String, long[]> stats = new LinkedHashMap<>();
        turnos.stream().filter(t -> t.getServicio() != null).forEach(t -> {
            String nombre = t.getServicio().getNombre();
            stats.computeIfAbsent(nombre, k -> new long[]{0});
            stats.get(nombre)[0]++;
        });

        return stats.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .map(e -> new TopServicioItem(e.getKey(), e.getValue()[0], BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(UUID empresaId, LocalDate desde, LocalDate hasta) throws IOException {
        List<TurnoEstado> todos = Arrays.asList(TurnoEstado.values());
        var turnos = turnoRepository.findByEmpresaIdAndEstadoInAndFechaRange(empresaId, todos, desde, hasta);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Turnos");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Cliente", "Servicio", "Fecha", "Hora", "Estado", "Observaciones"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (var turno : turnos) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(turno.getId().toString());
                row.createCell(1).setCellValue(turno.getCliente() != null ? turno.getCliente().getNombre() : "");
                row.createCell(2).setCellValue(turno.getServicio() != null ? turno.getServicio().getNombre() : "");
                row.createCell(3).setCellValue(turno.getFechaConfirmada() != null ? turno.getFechaConfirmada().toString() : "");
                row.createCell(4).setCellValue(turno.getHoraConfirmada() != null ? turno.getHoraConfirmada().toString() : "");
                row.createCell(5).setCellValue(turno.getEstado().name());
                row.createCell(6).setCellValue(turno.getObservaciones() != null ? turno.getObservaciones() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Excel exportado: empresaId={}, registros={}", empresaId, turnos.size());
            return out.toByteArray();
        }
    }
}
