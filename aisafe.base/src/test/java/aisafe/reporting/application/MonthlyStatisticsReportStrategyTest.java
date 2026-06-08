package aisafe.reporting.application;

import aisafe.aircontrolarea.domain.AirControlArea;
import aisafe.aircontrolarea.domain.AirControlAreaCode;
import aisafe.aircontrolarea.domain.GeoBoundary;
import aisafe.aircraft.domain.RegistrationNumber;
import aisafe.airport.domain.Airport;
import aisafe.airport.domain.AirportIATACode;
import aisafe.airport.domain.AirportICAOCode;
import aisafe.airport.domain.GeoCoordinate;
import aisafe.airtransportcompany.domain.IATACode;
import aisafe.dsl.ast.FlightType;
import aisafe.flightplan.domain.FlightPlan;
import aisafe.flightplan.domain.FlightPlanDesignator;
import aisafe.flightplan.domain.FuelQuantity;
import aisafe.flightroute.domain.FlightRoute;
import aisafe.flightroute.domain.RouteName;
import aisafe.infrastructure.persistence.inmemory.InMemoryAirportRepository;
import aisafe.infrastructure.persistence.inmemory.InMemoryFlightPlanRepository;
import aisafe.infrastructure.persistence.inmemory.InMemoryFlightRouteRepository;
import aisafe.infrastructure.persistence.inmemory.InMemoryWeatherDataRepository;
import aisafe.usermanagement.domain.AiSafePasswordPolicy;
import aisafe.usermanagement.domain.AiSafeRoles;
import aisafe.weatherdata.domain.WeatherData;
import aisafe.weatherdata.domain.WeatherSource;
import eapli.framework.infrastructure.authz.domain.model.PlainTextEncoder;
import eapli.framework.infrastructure.authz.domain.model.SystemUserBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthlyStatisticsReportStrategyTest {

    @Test
    void ensureMonthlyStatisticsAreComputedForTheOperatorsArea() {
        final AirControlArea northernPortugal = new AirControlArea(
                AirControlAreaCode.valueOf("PT-N"),
                "Northern Portugal",
                1200.0,
                new GeoBoundary(42.15, 36.95, -6.18, -9.50)
        );
        final AirControlArea centralSpain = new AirControlArea(
                AirControlAreaCode.valueOf("ES-C"),
                "Central Spain",
                900.0,
                new GeoBoundary(42.50, 39.00, -1.50, -5.00)
        );

        final InMemoryAirportRepository airportRepository = new InMemoryAirportRepository();
        final Airport opo = airportRepository.save(new Airport(
                AirportIATACode.valueOf("OPO"),
                AirportICAOCode.valueOf("LPPR"),
                "Porto Airport",
                "Porto",
                "Portugal",
                new GeoCoordinate(41.24, -8.68),
                69,
                northernPortugal
        ));
        final Airport lis = airportRepository.save(new Airport(
                AirportIATACode.valueOf("LIS"),
                AirportICAOCode.valueOf("LPPT"),
                "Lisbon Airport",
                "Lisbon",
                "Portugal",
                new GeoCoordinate(38.77, -9.13),
                114,
                northernPortugal
        ));
        final Airport mad = airportRepository.save(new Airport(
                AirportIATACode.valueOf("MAD"),
                AirportICAOCode.valueOf("LEMD"),
                "Madrid Airport",
                "Madrid",
                "Spain",
                new GeoCoordinate(40.47, -3.56),
                610,
                centralSpain
        ));
        final Airport bcn = airportRepository.save(new Airport(
                AirportIATACode.valueOf("BCN"),
                AirportICAOCode.valueOf("LEBL"),
                "Barcelona Airport",
                "Barcelona",
                "Spain",
                new GeoCoordinate(41.29, 2.08),
                4,
                centralSpain
        ));

        final InMemoryFlightRouteRepository routeRepository = new InMemoryFlightRouteRepository();
        routeRepository.save(new FlightRoute(
                new RouteName("TP100"),
                opo.identity(),
                mad.identity(),
                IATACode.valueOf("TP")
        ));
        routeRepository.save(new FlightRoute(
                new RouteName("TP200"),
                mad.identity(),
                lis.identity(),
                IATACode.valueOf("TP")
        ));
        routeRepository.save(new FlightRoute(
                new RouteName("TP300"),
                bcn.identity(),
                mad.identity(),
                IATACode.valueOf("TP")
        ));

        final InMemoryFlightPlanRepository flightPlanRepository = new InMemoryFlightPlanRepository();
        final FlightPlan validatedPlan = flightPlanRepository.save(new FlightPlan(
                FlightPlanDesignator.valueOf("TP9001"),
                FlightType.REGULAR,
                new RouteName("TP100"),
                RegistrationNumber.valueOf("CS-TUA"),
                1L,
                LocalDateTime.of(2030, 6, 10, 10, 0),
                FuelQuantity.valueOf(2500)
        ));
        validatedPlan.markValidated();

        final FlightPlan testedPlan = flightPlanRepository.save(new FlightPlan(
                FlightPlanDesignator.valueOf("TP9002"),
                FlightType.CHARTER,
                new RouteName("TP200"),
                RegistrationNumber.valueOf("CS-TUB"),
                2L,
                LocalDateTime.of(2030, 6, 12, 12, 30),
                FuelQuantity.valueOf(2700)
        ));
        testedPlan.markValidated();
        testedPlan.markTested();

        flightPlanRepository.save(new FlightPlan(
                FlightPlanDesignator.valueOf("TP9003"),
                FlightType.REGULAR,
                new RouteName("TP300"),
                RegistrationNumber.valueOf("CS-TUC"),
                3L,
                LocalDateTime.of(2030, 6, 14, 9, 0),
                FuelQuantity.valueOf(2000)
        ));

        flightPlanRepository.save(new FlightPlan(
                FlightPlanDesignator.valueOf("TP9004"),
                FlightType.REGULAR,
                new RouteName("TP100"),
                RegistrationNumber.valueOf("CS-TUD"),
                4L,
                LocalDateTime.of(2030, 7, 1, 9, 0),
                FuelQuantity.valueOf(2100)
        ));

        final InMemoryWeatherDataRepository weatherRepository = new InMemoryWeatherDataRepository();
        weatherRepository.save(new WeatherData(
                northernPortugal.identity(),
                new WeatherSource("IPMA", "CSV"),
                LocalDateTime.of(2030, 6, 10, 8, 0),
                20.0,
                15.0,
                "NE",
                1012.0,
                10.0
        ));
        weatherRepository.save(new WeatherData(
                northernPortugal.identity(),
                new WeatherSource("IPMA", "CSV"),
                LocalDateTime.of(2030, 6, 12, 8, 0),
                24.0,
                9.0,
                "NW",
                1015.0,
                8.0
        ));
        weatherRepository.save(new WeatherData(
                northernPortugal.identity(),
                new WeatherSource("IPMA", "CSV"),
                LocalDateTime.of(2030, 7, 1, 8, 0),
                30.0,
                6.0,
                "N",
                1011.0,
                12.0
        ));

        final MonthlyStatisticsReportStrategy strategy = new MonthlyStatisticsReportStrategy(
                flightPlanRepository,
                routeRepository,
                airportRepository,
                weatherRepository
        );

        final ReportData reportData = strategy.generate(
                YearMonth.of(2030, 6),
                northernPortugal,
                new SystemUserBuilder(new AiSafePasswordPolicy(), new PlainTextEncoder())
                        .withUsername("fco-us112")
                        .withPassword("Password1")
                        .withName("Flight", "Operator")
                        .withEmail("fco-us112@aisafe.com")
                        .withRoles(AiSafeRoles.FLIGHT_CONTROL_OPERATOR)
                        .build()
        );

        assertEquals("Monthly Statistics Report", reportData.reportType());
        assertEquals("PT-N", reportData.areaCode());
        assertEquals(4, reportData.sections().size());

        final List<String> summaryLines = reportData.sections().get(0).lines();
        assertTrue(summaryLines.contains("Total flight plans in month : 2"));
        assertTrue(summaryLines.contains("Departures from area        : 1"));
        assertTrue(summaryLines.contains("Arrivals to area           : 1"));
        assertTrue(summaryLines.contains("Weather records in month   : 2"));

        final List<String> weatherLines = reportData.sections().get(2).lines();
        assertTrue(weatherLines.contains("Total records        : 2"));
        assertTrue(weatherLines.contains("Average temperature : 22.0 C"));
        assertTrue(weatherLines.contains("Average wind speed  : 12.0"));
        assertTrue(weatherLines.contains("Lowest visibility   : 8.0"));

        final String formatted = new OperationalReportFormatter().format(reportData);
        assertTrue(formatted.contains("AISAFE"));
        assertTrue(formatted.contains("Monthly Statistics Report"));
        assertTrue(formatted.contains("Simple Graphics"));
    }
}
