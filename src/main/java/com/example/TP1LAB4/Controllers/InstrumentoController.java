package com.example.TP1LAB4.Controllers;

import com.example.TP1LAB4.Entities.Instrumento;
import com.example.TP1LAB4.Funciones.FuncionApp;
import com.example.TP1LAB4.Gestor.ChartManager;
import com.example.TP1LAB4.Services.Impl.InstrumentoServiceImpl;
import com.example.TP1LAB4.Services.InstrumentoService;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "api/v1/Instrumentos")
public class InstrumentoController extends BaseControllerImpl<Instrumento, InstrumentoServiceImpl> {

    @GetMapping("DataBar")
    public List<List<Object>> getDataChartBar() throws SQLException {
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("Mes y Año", "Cantidad de Pedidos"));

        ChartManager mChart = new ChartManager();
        ResultSet rs = mChart.getDatosChartByMonthAndYear();
        while (rs.next()) {
            data.add(Arrays.asList(rs.getString("mes_anio"), rs.getInt("cantidad_pedidos")));
        }
        return data;
    }


    @GetMapping("DataPie")
    public List<List<Object>> getDataChartPie() throws SQLException {
        List<List<Object>> data = new ArrayList<>();
        data.add(Arrays.asList("Instrumento", "Cantidad de Pedidos"));

        ChartManager mChart = new ChartManager();
        ResultSet rs = mChart.getDatosChartByInstrumento();
        while (rs.next()) {
            data.add(Arrays.asList(rs.getString("instrumento"), rs.getInt("cantidad_pedidos")));
        }
        return data;
    }


    @Autowired
    private InstrumentoService instrumentoService;
    @GetMapping("/pdf/{id}")
    public ResponseEntity<?> generarPDF(@PathVariable Long id) {
        try {
            Instrumento instrumento = instrumentoService.findById(id);

            // Validación si el instrumento no existe
            if (instrumento == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\":\"Instrumento con ID " + id + " no encontrado\"}");
            }

            ByteArrayOutputStream baos = generarPDFDesdeInstrumento(instrumento);

            // Construimos el nombre del archivo PDF
            String nombreArchivo = "Instrumento_" + instrumento.getId() + ".pdf";

            // Devolvemos el PDF como un array de bytes
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"")
                    .body(baos.toByteArray());

        } catch (IOException e) {
            // Registro de error con información detallada para IOExceptions
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Error de E/S al generar el PDF para el instrumento con ID " + id + "\"}");
        } catch (Exception e) {
            // Registro de error con información detallada para cualquier otra excepción
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"No se pudo generar el PDF para el instrumento con ID " + id + "\"}");
        }
    }

    private ByteArrayOutputStream generarPDFDesdeInstrumento(Instrumento instrumento) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Título
            document.add(new Paragraph(instrumento.getInstrumento())
                    .setFontSize(28)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12));


            // Sección de 2 columnas 60% imagen (izquierda) / 40% información del instrumento (derecha)
            float[] columnWidths = {60, 40}; // Anchos de las columnas en porcentaje
            Table table = new Table(columnWidths);
            table.setWidth(UnitValue.createPercentValue(90));

            // COLUMNA IZQUIERDA (60%) (imagen)
            Image imagen = null;
            try {
                imagen = new Image(ImageDataFactory.create(new URL(instrumento.getImagen())));
            } catch (Exception e) {
                // Manejo de errores al cargar la imagen
                imagen = new Image(ImageDataFactory.create(new URL("https://dummyimage.com/400x600/777/fff.jpg&text=:("))); // Imagen por defecto
            }
            // Escalar la imagen para ajustar al 60% del ancho de la celda
            imagen.scaleToFit(pdf.getDefaultPageSize().getWidth() * 0.5f, pdf.getDefaultPageSize().getHeight());
            Cell imageCell = new Cell().add(imagen).setBorder(Border.NO_BORDER);
            table.addCell(imageCell);

            // COLUMNA DERECHA (40%) (información del instrumento)
            Cell infoCell = new Cell().setBorder(Border.NO_BORDER);
            infoCell.add(new Paragraph(String.valueOf(instrumento.getCantidadVendida()) + " vendidos")
                    .setFontSize(8));
            infoCell.add(new Paragraph(instrumento.getInstrumento())
                    .setFontSize(16)
                    .setBold());
            infoCell.add(new Paragraph(String.format("$ %.2f", instrumento.getPrecio()))
                    .setFontSize(28));
            infoCell.add(new Paragraph("Marca: " + instrumento.getMarca())
                    .setBold()
                    .setMarginTop(12)); // Ajusta el valor según sea necesario
            infoCell.add(new Paragraph("Modelo: " + instrumento.getModelo())
                    .setBold()
                    .setMarginBottom(16)); // Ajusta el valor según sea necesario
            infoCell.add(new Paragraph("Categoría: " + instrumento.getCategoria().getDenominacion())
                    .setFontSize(12)
                    .setItalic());
            infoCell.add(new Paragraph("Costo Envío: ")
                    .setFontSize(12)
                    .setBold()
                    .setMarginTop(16));
            // Definir color verde
            Color colorVerde = new DeviceRgb(0, 128, 0); // RGB para verde
            // Icono shipping y texto de costo de envío
            String costoEnvio = instrumento.getCostoEnvio().equals("G") ? "Envío gratis" : "$ " + instrumento.getCostoEnvio();
            Paragraph costoEnvioParagraph = new Paragraph()
                    .setFontColor(colorVerde)
                    .add(costoEnvio)
                    .setBold();
            infoCell.add(costoEnvioParagraph);

            table.addCell(infoCell);

            // Añadir la tabla al documento
            document.add(table);

            // Debajo de las 2 columnas sigue el documento con UNA SOLA columna

            // Descripción
            document.add(new Paragraph("\nDescripción:")
                    .setBold()
                    .setMarginTop(12));
            document.add(new Paragraph(instrumento.getDescripcion()));

        } catch (Exception e) {
            throw new IOException("Error al generar el PDF", e);
        }

        return baos;
    }




}
